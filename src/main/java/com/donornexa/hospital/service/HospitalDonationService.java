package com.donornexa.hospital.service;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.hospital.dto.RecordDonationDTO;
import com.donornexa.hospital.dto.UpdateDonationStatusDTO;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.donation.repository.DonationRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalDonationService {

    private final DonationRepository donationRepository;
    private final HospitalRepository hospitalRepository;
    private final DonorRepository donorRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalInventoryService hospitalInventoryService;
    private final com.donornexa.matching.repository.RequestMatchRepository requestMatchRepository;
    private final com.donornexa.audit.repository.AuditLogRepository auditLogRepository;
    private final com.donornexa.notification.service.NotificationService notificationService;

    @Transactional(readOnly = true)
    public Hospital getHospitalByEmail(String email) {
        return hospitalRepository.findByUserEmailWithDetails(email.toLowerCase().trim())
                .orElseThrow(() -> new HospitalNotFoundException("Hospital facility not found for user: " + email));
    }

    @Transactional(readOnly = true)
    public Page<Donation> getHospitalDonations(String email, String query, DonationStatus status, Pageable pageable) {
        Hospital hospital = getHospitalByEmail(email);
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return donationRepository.searchByHospitalAndStatus(hospital.getId(), cleanQuery, status, pageable);
    }

    @Transactional(readOnly = true)
    public Donation getDonationByIdForHospital(String email, Long donationId) {
        Hospital hospital = getHospitalByEmail(email);
        Donation donation = donationRepository.findByIdWithDetails(donationId)
                .orElseThrow(() -> new HospitalOperationException("Donation record not found with ID: " + donationId));

        if (!donation.getHospital().getId().equals(hospital.getId())) {
            log.warn("IDOR attempt: Hospital {} attempted to access donation {} owned by hospital {}",
                    hospital.getId(), donationId, donation.getHospital().getId());
            throw new HospitalOperationException("Access Denied: You are not authorized to view or modify donation records from other hospital facilities.");
        }
        return donation;
    }

    @Transactional(readOnly = true)
    public List<Donor> getVerifiedDonors() {
        return donorRepository.findAllVerifiedWithUser();
    }

    @Transactional(readOnly = true)
    public List<BloodRequest> getActiveRequestsForHospital(Long hospitalId) {
        return bloodRequestRepository.findByHospitalId(hospitalId).stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING ||
                             r.getStatus() == RequestStatus.MATCHING ||
                             r.getStatus() == RequestStatus.PARTIALLY_FULFILLED)
                .toList();
    }

    @Transactional
    public Donation recordDonation(String email, RecordDonationDTO form) {
        Hospital hospital = getHospitalByEmail(email);

        Donor donor = donorRepository.findById(form.getDonorId())
                .orElseThrow(() -> new HospitalOperationException("Selected voluntary donor was not found (ID: " + form.getDonorId() + ")"));

        if (!Boolean.TRUE.equals(donor.getVerified())) {
            throw new HospitalOperationException("Clinical safety constraint: Donations may only be recorded for verified donors.");
        }

        BloodRequest bloodRequest = null;
        if (form.getBloodRequestId() != null) {
            bloodRequest = bloodRequestRepository.findById(form.getBloodRequestId())
                    .orElseThrow(() -> new HospitalOperationException("Linked blood request not found"));
            if (!bloodRequest.getHospital().getId().equals(hospital.getId())) {
                throw new HospitalOperationException("Cannot link donation to a blood request created by a different hospital.");
            }
        }

        LocalDateTime donationTime = form.getDonationDate() != null ? form.getDonationDate() : LocalDateTime.now();
        if (donationTime.isAfter(LocalDateTime.now())) {
            throw new HospitalOperationException("Donation timestamp cannot be recorded in the future.");
        }

        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setHospital(hospital);
        donation.setBloodRequest(bloodRequest);
        donation.setUnitsDonated(form.getUnitsDonated());
        donation.setDonationDate(donationTime);
        donation.setDonationStatus(form.getDonationStatus() != null ? form.getDonationStatus() : DonationStatus.COMPLETED);
        donation.setNotes(form.getNotes());
        donation.setInventoryAdded(false);

        Donation savedDonation = donationRepository.save(donation);
        log.info("Recorded donation ID {} for donor {} at hospital {}",
                savedDonation.getId(), donor.getId(), hospital.getId());

        // If completed at creation time:
        if (savedDonation.getDonationStatus() == DonationStatus.COMPLETED) {
            donor.setLastDonationDate(donationTime.toLocalDate());
            donorRepository.save(donor);

            if (form.isAddToInventory()) {
                hospitalInventoryService.addStockFromDonation(hospital, savedDonation);
                savedDonation.setInventoryAdded(true);
                savedDonation = donationRepository.save(savedDonation);
            }

            if (bloodRequest != null) {
                int currentFulfilled = bloodRequest.getUnitsFulfilled() != null ? bloodRequest.getUnitsFulfilled() : 0;
                int newFulfilled = currentFulfilled + savedDonation.getUnitsDonated();
                bloodRequest.setUnitsFulfilled(newFulfilled);
                if (newFulfilled >= bloodRequest.getUnitsRequired()) {
                    bloodRequest.setStatus(RequestStatus.FULFILLED);
                } else {
                    bloodRequest.setStatus(RequestStatus.PARTIALLY_FULFILLED);
                }
                bloodRequestRepository.save(bloodRequest);
            }
        }

        // Notify donor of recorded donation
        try {
            if (donor.getUser() != null) {
                String title = "Blood Donation Recorded (#" + savedDonation.getId() + ")";
                String message = String.format("Your donation of %d unit(s) of %s blood at %s has been recorded with status %s.",
                        savedDonation.getUnitsDonated(),
                        donor.getBloodGroup().name().replace('_', ' '),
                        hospital.getHospitalName(),
                        savedDonation.getDonationStatus());
                notificationService.createNotification(donor.getUser(), title, message, com.donornexa.common.enums.NotificationType.DONATION_UPDATE);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch donation notification to donor: {}", ex.getMessage());
        }

        return savedDonation;
    }

    @Transactional
    public Donation updateDonationStatus(String email, Long donationId, UpdateDonationStatusDTO form) {
        Donation donation = getDonationByIdForHospital(email, donationId);
        Hospital hospital = donation.getHospital();

        if (donation.getDonationStatus() == DonationStatus.COMPLETED) {
            throw new HospitalOperationException("This donation is already marked COMPLETED and clinical audit records are sealed.");
        }

        DonationStatus targetStatus = form.getStatus();
        donation.setDonationStatus(targetStatus);
        if (form.getNotes() != null && !form.getNotes().isBlank()) {
            donation.setNotes(form.getNotes());
        }

        if (targetStatus == DonationStatus.COMPLETED) {
            donation.getDonor().setLastDonationDate(donation.getDonationDate().toLocalDate());
            donorRepository.save(donation.getDonor());

            if (form.isAddToInventory() && !Boolean.TRUE.equals(donation.getInventoryAdded())) {
                hospitalInventoryService.addStockFromDonation(hospital, donation);
                donation.setInventoryAdded(true);
            }

            if (donation.getBloodRequest() != null) {
                BloodRequest br = donation.getBloodRequest();
                int currentFulfilled = br.getUnitsFulfilled() != null ? br.getUnitsFulfilled() : 0;
                int newFulfilled = currentFulfilled + donation.getUnitsDonated();
                br.setUnitsFulfilled(newFulfilled);
                if (newFulfilled >= br.getUnitsRequired()) {
                    br.setStatus(RequestStatus.FULFILLED);
                } else {
                    br.setStatus(RequestStatus.PARTIALLY_FULFILLED);
                }
                bloodRequestRepository.save(br);
            }
        }

        Donation updated = donationRepository.save(donation);
        log.info("Updated donation ID {} to status {} by hospital {}", donationId, targetStatus, hospital.getId());

        // Notify donor of donation status change
        try {
            if (updated.getDonor() != null && updated.getDonor().getUser() != null) {
                String title = "Donation Status Updated (#" + donationId + ")";
                String message = String.format("Your blood donation session (#%d) at %s was updated to %s.",
                        donationId,
                        hospital.getHospitalName(),
                        targetStatus);
                notificationService.createNotification(updated.getDonor().getUser(), title, message, com.donornexa.common.enums.NotificationType.DONATION_UPDATE);
            }
        } catch (Exception ex) {
            log.warn("Failed to notify donor of donation status update: {}", ex.getMessage());
        }

        return updated;
    }

    @Transactional
    public Donation addDonationToInventory(String email, Long donationId) {
        Donation donation = getDonationByIdForHospital(email, donationId);
        Hospital hospital = donation.getHospital();

        if (donation.getDonationStatus() != DonationStatus.COMPLETED) {
            throw new HospitalOperationException("Only COMPLETED donations can be registered into blood bank inventory.");
        }

        if (Boolean.TRUE.equals(donation.getInventoryAdded())) {
            throw new HospitalOperationException("This donation has already been converted and added to inventory.");
        }

        hospitalInventoryService.addStockFromDonation(hospital, donation);
        donation.setInventoryAdded(true);
        return donationRepository.save(donation);
    }

    // =========================================================================
    // Donor Assignment & Coordination Lifecycle (Phases 6 - 8 Workflow)
    // =========================================================================

    @Transactional
    public com.donornexa.matching.entity.RequestMatch scheduleMatchAppointment(
            String email, Long matchId, LocalDateTime appointmentTime, String notes) {
        Hospital hospital = getHospitalByEmail(email);
        com.donornexa.matching.entity.RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new HospitalOperationException("Match record not found with ID: " + matchId));

        if (!match.getBloodRequest().getHospital().getId().equals(hospital.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this blood request.");
        }

        match.setMatchStatus(com.donornexa.common.enums.MatchStatus.SCHEDULED);
        match.setScheduledAppointmentTime(appointmentTime != null ? appointmentTime : LocalDateTime.now().plusHours(2));
        if (notes != null && !notes.isBlank()) {
            match.setAppointmentNotes(notes.trim());
        }

        com.donornexa.matching.entity.RequestMatch saved = requestMatchRepository.save(match);
        log.info("Match #{} scheduled for appointment at {} by hospital {}", matchId, saved.getScheduledAppointmentTime(), hospital.getId());

        // Notify Donor
        try {
            if (saved.getDonor().getUser() != null) {
                String title = "Donation Appointment Scheduled";
                String message = String.format("%s has scheduled your donation appointment for %s regarding Blood Request #%d. Notes: %s",
                        hospital.getHospitalName(),
                        saved.getScheduledAppointmentTime().toString().replace('T', ' '),
                        saved.getBloodRequest().getId(),
                        saved.getAppointmentNotes() != null ? saved.getAppointmentNotes() : "Please arrive on time.");
                notificationService.createNotification(saved.getDonor().getUser(), title, message, com.donornexa.common.enums.NotificationType.DONATION_UPDATE);
            }
        } catch (Exception ex) {
            log.warn("Failed to notify donor of scheduled appointment: {}", ex.getMessage());
        }

        return saved;
    }

    @Transactional
    public com.donornexa.matching.entity.RequestMatch markMatchArrived(String email, Long matchId) {
        Hospital hospital = getHospitalByEmail(email);
        com.donornexa.matching.entity.RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new HospitalOperationException("Match record not found with ID: " + matchId));

        if (!match.getBloodRequest().getHospital().getId().equals(hospital.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this blood request.");
        }

        match.setMatchStatus(com.donornexa.common.enums.MatchStatus.ARRIVED);
        com.donornexa.matching.entity.RequestMatch saved = requestMatchRepository.save(match);
        log.info("Donor #{} marked ARRIVED for match #{} by hospital {}", saved.getDonor().getId(), matchId, hospital.getId());
        return saved;
    }

    @Transactional
    public com.donornexa.matching.entity.RequestMatch markMatchInProgress(String email, Long matchId) {
        Hospital hospital = getHospitalByEmail(email);
        com.donornexa.matching.entity.RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new HospitalOperationException("Match record not found with ID: " + matchId));

        if (!match.getBloodRequest().getHospital().getId().equals(hospital.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this blood request.");
        }

        match.setMatchStatus(com.donornexa.common.enums.MatchStatus.DONATION_IN_PROGRESS);
        com.donornexa.matching.entity.RequestMatch saved = requestMatchRepository.save(match);
        log.info("Donation marked IN_PROGRESS for match #{} by hospital {}", matchId, hospital.getId());
        return saved;
    }

    @Transactional
    public com.donornexa.matching.entity.RequestMatch markMatchNoShow(String email, Long matchId, String reason) {
        Hospital hospital = getHospitalByEmail(email);
        com.donornexa.matching.entity.RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new HospitalOperationException("Match record not found with ID: " + matchId));

        if (!match.getBloodRequest().getHospital().getId().equals(hospital.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this blood request.");
        }

        match.setMatchStatus(com.donornexa.common.enums.MatchStatus.NO_SHOW);
        if (reason != null && !reason.isBlank()) {
            match.setAppointmentNotes(reason.trim());
        }
        com.donornexa.matching.entity.RequestMatch saved = requestMatchRepository.save(match);
        log.info("Match #{} marked NO_SHOW by hospital {}", matchId, hospital.getId());
        return saved;
    }

    @Transactional
    public Donation confirmMatchDonation(String email, Long matchId, Integer unitsDonated, String notes, boolean addToInventory) {
        Hospital hospital = getHospitalByEmail(email);
        com.donornexa.matching.entity.RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new HospitalOperationException("Match record not found with ID: " + matchId));

        if (!match.getBloodRequest().getHospital().getId().equals(hospital.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this blood request.");
        }

        if (match.getMatchStatus() == com.donornexa.common.enums.MatchStatus.CONFIRMED ||
            match.getMatchStatus() == com.donornexa.common.enums.MatchStatus.COMPLETED) {
            throw new HospitalOperationException("This donation assignment has already been confirmed.");
        }

        int units = (unitsDonated != null && unitsDonated >= 1) ? unitsDonated : 1;
        Donor donor = match.getDonor();
        
        // Concurrency-safe pessimistic write lock on the BloodRequest
        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(match.getBloodRequest().getId())
                .orElse(match.getBloodRequest());

        int currentFulfilled = bloodRequest.getUnitsFulfilled() != null ? bloodRequest.getUnitsFulfilled() : 0;
        if (currentFulfilled >= bloodRequest.getUnitsRequired()) {
            throw new HospitalOperationException("Blood request #" + bloodRequest.getId() + " is already fully fulfilled ("
                    + currentFulfilled + "/" + bloodRequest.getUnitsRequired() + " units).");
        }

        // 1. Create and save official Donation record
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setHospital(hospital);
        donation.setBloodRequest(bloodRequest);
        donation.setUnitsDonated(units);
        donation.setDonationDate(LocalDateTime.now());
        donation.setDonationStatus(DonationStatus.COMPLETED);
        donation.setNotes(notes != null && !notes.isBlank() ? notes.trim() : "Verified blood donation confirmed by hospital.");
        donation.setInventoryAdded(false);

        Donation savedDonation = donationRepository.save(donation);

        // 2. Ingest units into hospital blood inventory
        if (addToInventory) {
            hospitalInventoryService.addStockFromDonation(hospital, savedDonation);
            savedDonation.setInventoryAdded(true);
            savedDonation = donationRepository.save(savedDonation);
        }

        // 3. Update RequestMatch assignment record
        match.setMatchStatus(com.donornexa.common.enums.MatchStatus.CONFIRMED);
        match.setUnitsDonated(units);
        match.setDonation(savedDonation);
        requestMatchRepository.save(match);

        // 4. Update Donor's last donation date (56-day cooldown reset)
        donor.setLastDonationDate(donation.getDonationDate().toLocalDate());
        donorRepository.save(donor);

        // 5. Update main BloodRequest fulfillment counts and lifecycle status
        int newFulfilled = currentFulfilled + units;
        bloodRequest.setUnitsFulfilled(newFulfilled);

        if (newFulfilled >= bloodRequest.getUnitsRequired()) {
            bloodRequest.setStatus(RequestStatus.FULFILLED);
        } else {
            bloodRequest.setStatus(RequestStatus.PARTIALLY_FULFILLED);
        }
        bloodRequestRepository.save(bloodRequest);

        log.info("Donation confirmed for match #{}: {} unit(s) of {} added to hospital {}. Request #{} is now {}",
                matchId, units, donor.getBloodGroup(), hospital.getId(), bloodRequest.getId(), bloodRequest.getStatus());

        // 6. Notify Donor
        try {
            if (donor.getUser() != null) {
                String title = "Blood Donation Confirmed! ðŸŽ‰";
                String msg = String.format("Hospital %s has officially confirmed your donation of %d unit(s) of %s blood for Request #%d. Thank you for saving a life!",
                        hospital.getHospitalName(), units, donor.getBloodGroup().name().replace('_', ' '), bloodRequest.getId());
                notificationService.createNotification(donor.getUser(), title, msg, com.donornexa.common.enums.NotificationType.DONATION_UPDATE);
            }
        } catch (Exception ex) {
            log.warn("Failed to notify donor of confirmed donation: {}", ex.getMessage());
        }

        return savedDonation;
    }
}
