package com.donornexa.matching.service;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.ConversationStatus;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.MatchStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.matching.repository.RequestMatchRepository;
import com.donornexa.messaging.entity.Conversation;
import com.donornexa.messaging.repository.ConversationRepository;
import com.donornexa.user.entity.User;

import com.donornexa.donor.dto.DonorEligibilityResult;
import com.donornexa.donor.service.DonorEligibilityService;
import com.donornexa.matching.dto.DonorMatchDTO;
import com.donornexa.matching.dto.MatchingEvaluationSummaryDTO;
import com.donornexa.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core matching and recommendation engine for blood requests.
 * Evaluates donor medical eligibility, operational availability,
 * verification standing, geographic proximity, and ABO/Rh compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonorMatchingService {

    private final BloodRequestRepository bloodRequestRepository;
    private final DonorRepository donorRepository;
    private final RequestMatchRepository requestMatchRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodCompatibilityService bloodCompatibilityService;
    private final DonorEligibilityService donorEligibilityService;
    private final NotificationService notificationService;
    private final ConversationRepository conversationRepository;

    /**
     * Executes the donor matching algorithm for a blood request, generates ranked matches,
     * persists them idempotently, sends notifications, and updates request status.
     *
     * @param requestId ID of the blood request
     * @param hospitalEmail Authenticated hospital user email (or admin bypass)
     * @return Detailed evaluation summary and matched donors
     */
    @Transactional
    public MatchingEvaluationSummaryDTO findAndSaveMatchesForRequest(Long requestId, String hospitalEmail) {
        BloodRequest request = bloodRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new NoSuchElementException("Blood request not found with ID: " + requestId));

        // Security check: Only the owning hospital (or admin) can trigger matching
        if (hospitalEmail != null) {
            Hospital hospital = hospitalRepository.findByUserEmailWithDetails(hospitalEmail.toLowerCase().trim())
                    .orElse(null);
            if (hospital != null) {
                if (!request.getHospital().getId().equals(hospital.getId())) {
                    throw new AccessDeniedException("You do not have permission to execute matching for this blood request.");
                }
                if (!Boolean.TRUE.equals(hospital.getVerified())) {
                    throw new AccessDeniedException("Hospital verification is pending or rejected. Only verified hospitals can execute matching.");
                }
            }
        }

        // Validate request lifecycle state
        if (request.getStatus() == RequestStatus.FULFILLED ||
            request.getStatus() == RequestStatus.CANCELLED ||
            request.getStatus() == RequestStatus.EXPIRED ||
            request.getStatus() == RequestStatus.REJECTED) {
            throw new IllegalStateException("Matching cannot be executed for request #" + requestId +
                    " because its status is " + request.getStatus());
        }

        // Transition PENDING/APPROVED requests to MATCHING
        if (request.getStatus() == RequestStatus.PENDING ||
            request.getStatus() == RequestStatus.PENDING_ADMIN_APPROVAL ||
            request.getStatus() == RequestStatus.APPROVED) {
            request.setStatus(RequestStatus.MATCHING);
            bloodRequestRepository.save(request);
            log.info("Blood request #{} transitioned to MATCHING", requestId);
        }

        // 1. Identify all compatible blood groups
        Set<BloodGroup> compatibleGroups = bloodCompatibilityService.getCompatibleDonorGroups(request.getBloodGroup());

        // 2. Query potential candidates from database
        List<Donor> allCandidates = donorRepository.findAllWithDetails();
        int totalEvaluated = allCandidates.size();

        int excludedIncompatible = 0;
        int excludedUnverified = 0;
        int excludedUnavailable = 0;
        int excludedMedical = 0;
        int excludedAccount = 0;

        List<ScoredDonor> scoredDonors = new ArrayList<>();

        for (Donor donor : allCandidates) {
            // Rule 1: ABO/Rh Compatibility
            if (!compatibleGroups.contains(donor.getBloodGroup())) {
                excludedIncompatible++;
                continue;
            }

            // Rule 2: Donor Verification Standing
            if (!Boolean.TRUE.equals(donor.getVerified())) {
                excludedUnverified++;
                continue;
            }

            // Rule 3: Operational Availability
            if (donor.getAvailability() != DonorAvailability.AVAILABLE) {
                excludedUnavailable++;
                continue;
            }

            // Rule 4: User Account Status
            User donorUser = donor.getUser();
            if (donorUser == null || donorUser.getAccountStatus() != AccountStatus.ACTIVE || !Boolean.TRUE.equals(donorUser.getEnabled())) {
                excludedAccount++;
                continue;
            }

            // Rule 5: Medical and Cooldown Eligibility (Age 18-60, Weight >= 50kg, Cooldown >= 90 days)
            DonorEligibilityResult eligibility = donorEligibilityService.evaluateEligibility(donor);
            if (!eligibility.isEligible()) {
                excludedMedical++;
                continue;
            }

            // Calculate Match Score and Explanation
            ScoredDonor scored = calculateScore(donor, request);
            scoredDonors.add(scored);
        }

        // 3. Sort by score descending (highest score first)
        scoredDonors.sort((a, b) -> Integer.compare(b.score, a.score));

        // 4. Persist matches idempotently and dispatch notifications
        List<DonorMatchDTO> matchDTOs = new ArrayList<>();

        for (ScoredDonor sd : scoredDonors) {
            Donor donor = sd.donor;
            Optional<RequestMatch> existingOpt = requestMatchRepository.findByDonorIdAndBloodRequestId(donor.getId(), request.getId());

            RequestMatch match;
            boolean isNew = false;

            if (existingOpt.isPresent()) {
                match = existingOpt.get();
                // Update score and summary while preserving response state
                match.setMatchScore(sd.score);
                match.setCompatibilitySummary(sd.summary);
            } else {
                match = new RequestMatch();
                match.setBloodRequest(request);
                match.setDonor(donor);
                match.setMatchStatus(MatchStatus.PENDING);
                match.setMatchScore(sd.score);
                match.setCompatibilitySummary(sd.summary);
                match.setMatchedAt(LocalDateTime.now());
                match.setNotified(false);
                isNew = true;
            }

            // Dispatch notification if not yet notified
            if (Boolean.FALSE.equals(match.getNotified())) {
                try {
                    String title = "Blood Donation Match: " + request.getBloodGroup().name().replace('_', ' ');
                    String message = String.format("Hospital %s in %s has an urgent request for %s blood. You are a matching donor! Check your matches to respond.",
                            request.getHospital().getHospitalName(),
                            request.getCity().getName(),
                            request.getBloodGroup().name().replace('_', ' '));

                    notificationService.createNotification(donor.getUser(), title, message, NotificationType.DONOR_MATCH);
                    match.setNotified(true);
                } catch (Exception ex) {
                    log.warn("Failed to dispatch match notification to donor {}: {}", donor.getUser().getEmail(), ex.getMessage());
                }
            }

            RequestMatch savedMatch = requestMatchRepository.save(match);
            matchDTOs.add(mapToDTO(savedMatch, request, donor, sd));
        }

        String summaryMessage;
        if (matchDTOs.isEmpty()) {
            summaryMessage = "No eligible donors were found for this blood request at the moment.";
        } else {
            summaryMessage = String.format("Found %d eligible donor match(es) for blood request #%d.", matchDTOs.size(), requestId);
        }

        log.info("Matching run completed for request #{}: evaluated={}, matched={}, incompatible={}, unverified={}, unavailable={}, medical={}, account={}",
                requestId, totalEvaluated, matchDTOs.size(), excludedIncompatible, excludedUnverified, excludedUnavailable, excludedMedical, excludedAccount);

        return MatchingEvaluationSummaryDTO.builder()
                .bloodRequestId(request.getId())
                .requiredBloodGroup(request.getBloodGroup())
                .urgency(request.getUrgency())
                .hospitalName(request.getHospital().getHospitalName())
                .hospitalCity(request.getCity().getName())
                .hospitalProvince(request.getProvince().getName())
                .totalDonorsEvaluated(totalEvaluated)
                .excludedIncompatible(excludedIncompatible)
                .excludedUnverified(excludedUnverified)
                .excludedUnavailable(excludedUnavailable)
                .excludedMedicalEligibility(excludedMedical)
                .excludedAccountInactive(excludedAccount)
                .totalMatchesFound(matchDTOs.size())
                .matches(matchDTOs)
                .message(summaryMessage)
                .build();
    }

    /**
     * Retrieves existing matches for a blood request without re-running matching.
     */
    @Transactional(readOnly = true)
    public MatchingEvaluationSummaryDTO getMatchesForRequest(Long requestId, String hospitalEmail) {
        BloodRequest request = bloodRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new NoSuchElementException("Blood request not found with ID: " + requestId));

        if (hospitalEmail != null) {
            Hospital hospital = hospitalRepository.findByUserEmailWithDetails(hospitalEmail.toLowerCase().trim()).orElse(null);
            if (hospital != null && !request.getHospital().getId().equals(hospital.getId())) {
                throw new AccessDeniedException("You do not have permission to view matches for this blood request.");
            }
        }

        List<RequestMatch> matches = requestMatchRepository.findByBloodRequestIdWithDetailsOrderByScoreDesc(requestId);
        List<DonorMatchDTO> matchDTOs = matches.stream()
                .map(m -> mapToDTO(m, request, m.getDonor(), calculateScore(m.getDonor(), request)))
                .collect(Collectors.toList());

        String message = matchDTOs.isEmpty()
                ? "No matching donors recorded yet. Click 'Find Matching Donors' to run the recommendation engine."
                : String.format("Showing %d donor match(es) for this request.", matchDTOs.size());

        return MatchingEvaluationSummaryDTO.builder()
                .bloodRequestId(request.getId())
                .requiredBloodGroup(request.getBloodGroup())
                .urgency(request.getUrgency())
                .hospitalName(request.getHospital().getHospitalName())
                .hospitalCity(request.getCity().getName())
                .hospitalProvince(request.getProvince().getName())
                .totalDonorsEvaluated(0)
                .totalMatchesFound(matchDTOs.size())
                .matches(matchDTOs)
                .message(message)
                .build();
    }

    /**
     * Records a donor's response (Accept or Decline) to a match request.
     */
    @Transactional
    public DonorMatchDTO respondToMatch(Long matchId, String donorEmail, boolean accept, String notes) {
        RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new NoSuchElementException("Match record not found with ID: " + matchId));

        // Strict Ownership Enforcement: authenticated user must be the matched donor
        if (!match.getDonor().getUser().getEmail().equalsIgnoreCase(donorEmail.trim())) {
            throw new AccessDeniedException("Access denied. You can only respond to matching requests sent to your account.");
        }

        // Response can only be set once from PENDING or INVITED state
        if (match.getMatchStatus() != MatchStatus.PENDING && match.getMatchStatus() != MatchStatus.INVITED) {
            throw new IllegalStateException("You have already responded to this blood match (Status: " + match.getMatchStatus() + ").");
        }

        // Concurrency-safe check when donor accepts
        if (accept) {
            BloodRequest request = bloodRequestRepository.findByIdForUpdate(match.getBloodRequest().getId())
                    .orElse(match.getBloodRequest());
            if (request.getStatus() == RequestStatus.FULFILLED ||
                (request.getUnitsFulfilled() != null && request.getUnitsFulfilled() >= request.getUnitsRequired())) {
                throw new IllegalStateException("Blood request #" + request.getId() + " has already been fully fulfilled by other donors.");
            }
        }

        match.setMatchStatus(accept ? MatchStatus.ACCEPTED : MatchStatus.REJECTED);
        match.setResponseAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            match.setDonorNotes(notes.trim());
        }

        RequestMatch saved = requestMatchRepository.save(match);
        log.info("Donor {} responded to match #{} with status: {}", donorEmail, matchId, saved.getMatchStatus());

        // Automatically establish or ensure a direct coordination channel upon acceptance
        if (accept) {
            try {
                if (!conversationRepository.existsByDonorIdAndHospitalIdAndBloodRequestId(
                        saved.getDonor().getId(), saved.getBloodRequest().getHospital().getId(), saved.getBloodRequest().getId())) {
                    Conversation conversation = new Conversation();
                    conversation.setDonor(saved.getDonor());
                    conversation.setHospital(saved.getBloodRequest().getHospital());
                    conversation.setBloodRequest(saved.getBloodRequest());
                    conversation.setStatus(com.donornexa.common.enums.ConversationStatus.ACTIVE);
                    conversation.setLastMessageAt(LocalDateTime.now());
                    conversationRepository.save(conversation);
                    log.info("Auto-established coordination conversation ID {} upon donor acceptance for Request #{}",
                            conversation.getId(), saved.getBloodRequest().getId());
                }
            } catch (Exception ex) {
                log.warn("Conversation establishment notice: {}", ex.getMessage());
            }
        }

        // Notify Hospital of donor response
        try {
            User hospitalUser = saved.getBloodRequest().getHospital().getUser();
            if (hospitalUser != null) {
                String action = accept ? "ACCEPTED" : "DECLINED";
                String title = "Donor " + action + " Blood Request #" + saved.getBloodRequest().getId();
                String message = String.format("Donor %s (%s) has %s your blood request for %s blood in %s.",
                        saved.getDonor().getUser().getFullName(),
                        saved.getDonor().getBloodGroup().name().replace('_', ' '),
                        action.toLowerCase(),
                        saved.getBloodRequest().getBloodGroup().name().replace('_', ' '),
                        saved.getBloodRequest().getCity().getName());

                notificationService.createNotification(hospitalUser, title, message, NotificationType.DONOR_MATCH);
            }
        } catch (Exception ex) {
            log.warn("Failed to notify hospital of donor response: {}", ex.getMessage());
        }

        return mapToDTO(saved, saved.getBloodRequest(), saved.getDonor(), calculateScore(saved.getDonor(), saved.getBloodRequest()));
    }

    /**
     * Retrieves paginated matches for the authenticated donor.
     */
    @Transactional(readOnly = true)
    public Page<DonorMatchDTO> getMatchesForDonor(String donorEmail, Pageable pageable) {
        Page<RequestMatch> page = requestMatchRepository.findByDonorUserEmailWithDetails(donorEmail.toLowerCase().trim(), pageable);
        return page.map(m -> mapToDTO(m, m.getBloodRequest(), m.getDonor(), calculateScore(m.getDonor(), m.getBloodRequest())));
    }

    /**
     * Search and filter match records across the system for administrator monitoring.
     */
    @Transactional(readOnly = true)
    public Page<DonorMatchDTO> searchMatchesForAdmin(String query, MatchStatus status, BloodGroup bloodGroup, Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        Page<RequestMatch> page = requestMatchRepository.searchAndFilter(cleanQuery, status, bloodGroup, pageable);
        return page.map(m -> mapToDTO(m, m.getBloodRequest(), m.getDonor(), calculateScore(m.getDonor(), m.getBloodRequest())));
    }

    /**
     * Transparent Match Score Calculation:
     * - Base score: 50 (passed all mandatory clinical/operational criteria)
     * - Proximity:
     *     Same City: +30
     *     Same Province (different city): +15
     *     Other Province: +0
     * - Blood precision:
     *     Exact identity match: +10
     *     Compatible cross-match: +5
     * - Donation freshness:
     *     First-time donor or last donation > 180 days: +5
     *     Last donation 90â€“180 days: +2
     * - Urgency boost:
     *     CRITICAL: +5
     *     URGENT: +3
     *     NORMAL: +0
     * Total score range: 50â€“100
     */
    private ScoredDonor calculateScore(Donor donor, BloodRequest request) {
        int score = 50;
        List<String> reasons = new ArrayList<>();

        // 1. Proximity
        boolean sameCity = donor.getCity() != null && request.getCity() != null &&
                donor.getCity().getId().equals(request.getCity().getId());
        boolean sameProvince = donor.getProvince() != null && request.getProvince() != null &&
                donor.getProvince().getId().equals(request.getProvince().getId());

        if (sameCity) {
            score += 30;
            reasons.add("Same city (" + request.getCity().getName() + ")");
        } else if (sameProvince) {
            score += 15;
            reasons.add("Same province (" + request.getProvince().getName() + ")");
        } else {
            reasons.add("Inter-province donor (" + (donor.getProvince() != null ? donor.getProvince().getName() : "Other") + ")");
        }

        // 2. Blood Precision
        boolean exactBlood = bloodCompatibilityService.isExactMatch(donor.getBloodGroup(), request.getBloodGroup());
        if (exactBlood) {
            score += 10;
            reasons.add("Exact blood match (" + donor.getBloodGroup().name().replace('_', ' ') + ")");
        } else {
            score += 5;
            reasons.add("Compatible cross-match (" + donor.getBloodGroup().name().replace('_', ' ') + " -> " + request.getBloodGroup().name().replace('_', ' ') + ")");
        }

        // 3. Donation interval buffer
        if (donor.getLastDonationDate() == null) {
            score += 5;
            reasons.add("First-time donor");
        } else {
            score += 3;
            reasons.add("Cooldown satisfied (90+ days)");
        }

        // 4. Urgency
        if (request.getUrgency() == RequestUrgency.CRITICAL) {
            score += 5;
            reasons.add("Critical urgency priority");
        } else if (request.getUrgency() == RequestUrgency.URGENT) {
            score += 3;
            reasons.add("Urgent priority");
        }

        // Cap at 100
        score = Math.min(100, score);
        String summary = String.join(" â€¢ ", reasons);

        return new ScoredDonor(donor, score, summary, sameCity, sameProvince, exactBlood);
    }

    private DonorMatchDTO mapToDTO(RequestMatch match, BloodRequest request, Donor donor, ScoredDonor scored) {
        String rawPhone = donor.getUser() != null ? donor.getUser().getPhoneNumber() : "";
        String maskedPhone = com.donornexa.common.util.PhoneMaskingUtil.maskPhoneNumber(rawPhone);

        return DonorMatchDTO.builder()
                .matchId(match.getId())
                .bloodRequestId(request.getId())
                .donorId(donor.getId())
                .donorName(donor.getUser() != null ? donor.getUser().getFullName() : "Donor #" + donor.getId())
                .donorEmail(donor.getUser() != null ? donor.getUser().getEmail() : "")
                .donorPhone(rawPhone)
                .maskedDonorPhone(maskedPhone)
                .donorBloodGroup(donor.getBloodGroup())
                .donorProvince(donor.getProvince() != null ? donor.getProvince().getName() : "")
                .donorCity(donor.getCity() != null ? donor.getCity().getName() : "")
                .availability(donor.getAvailability())
                .verified(Boolean.TRUE.equals(donor.getVerified()))
                .matchScore(match.getMatchScore() != null ? match.getMatchScore() : scored.score)
                .compatibilitySummary(match.getCompatibilitySummary() != null ? match.getCompatibilitySummary() : scored.summary)
                .matchStatus(match.getMatchStatus())
                .notified(Boolean.TRUE.equals(match.getNotified()))
                .matchedAt(match.getMatchedAt())
                .responseAt(match.getResponseAt())
                .donorNotes(match.getDonorNotes())
                .scheduledAppointmentTime(match.getScheduledAppointmentTime())
                .appointmentNotes(match.getAppointmentNotes())
                .unitsDonated(match.getUnitsDonated())
                .sameCity(scored.sameCity)
                .sameProvince(scored.sameProvince)
                .exactBloodMatch(scored.exactBlood)
                .build();
    }

    private static class ScoredDonor {
        final Donor donor;
        final int score;
        final String summary;
        final boolean sameCity;
        final boolean sameProvince;
        final boolean exactBlood;

        ScoredDonor(Donor donor, int score, String summary, boolean sameCity, boolean sameProvince, boolean exactBlood) {
            this.donor = donor;
            this.score = score;
            this.summary = summary;
            this.sameCity = sameCity;
            this.sameProvince = sameProvince;
            this.exactBlood = exactBlood;
        }
    }
}
