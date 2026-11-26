package com.donornexa.donor.service;


import com.donornexa.badge.service.DonationBadgeService;
import com.donornexa.badge.entity.DonationBadge;
import com.donornexa.donor.dto.DonorDashboardSummary;
import com.donornexa.donor.dto.DonorEligibilityResult;
import com.donornexa.donor.dto.DonorProfileUpdateRequest;
import com.donornexa.common.exception.DonorProfileException;
import com.donornexa.location.entity.City;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.donation.repository.DonationRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service orchestrating donor management operations:
 * profile retrieval, permitted profile updates, availability toggling,
 * eligibility calculation, and secure donation history tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonorService {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final DonorEligibilityService eligibilityService;
    private final com.donornexa.matching.repository.RequestMatchRepository requestMatchRepository;
    private final com.donornexa.notification.repository.NotificationRepository notificationRepository;
    private final com.donornexa.messaging.repository.MessageRepository messageRepository;
    private final com.donornexa.common.service.FileUploadService fileUploadService;

    /**
     * Retrieve the donor entity associated with the authenticated user's email,
     * eager-loading related user, province, and city entities.
     *
     * @param email Authenticated user email
     * @return Fully populated {@link Donor} entity
     * @throws DonorProfileException if the donor record does not exist
     */
    @Transactional(readOnly = true)
    public Donor getDonorByUserEmail(String email) {
        return donorRepository.findByUserEmailWithDetails(email)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new DonorProfileException("User account not found: " + email));
                    return donorRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new DonorProfileException("Donor profile not found for user: " + email));
                });
    }

    /**
     * Constructs a pre-populated profile update DTO from the donor's current entity state.
     *
     * @param email Authenticated user email
     * @return Pre-filled {@link DonorProfileUpdateRequest}
     */
    @Transactional(readOnly = true)
    public DonorProfileUpdateRequest getProfileUpdateRequest(String email) {
        Donor donor = getDonorByUserEmail(email);
        DonorProfileUpdateRequest request = new DonorProfileUpdateRequest();
        request.setFullName(donor.getUser().getFullName());
        request.setPhoneNumber(donor.getUser().getPhoneNumber());
        request.setWeight(donor.getWeight());
        request.setProvinceId(donor.getProvince().getId());
        request.setCityId(donor.getCity().getId());
        request.setCompleteAddress(donor.getCompleteAddress());
        return request;
    }

    /**
     * Updates permitted profile fields for the authenticated donor.
     * Guaranteed not to modify protected fields such as role, verification status,
     * blood group, date of birth, or email.
     *
     * @param email   Authenticated user email
     * @param request Validated update DTO
     * @return Updated {@link Donor} entity
     */
    @Transactional
    public Donor updateProfile(String email, DonorProfileUpdateRequest request) {
        Donor donor = getDonorByUserEmail(email);
        User user = donor.getUser();

        // 1. Validate phone number uniqueness if modified
        String newPhone = request.getPhoneNumber().trim();
        if (userRepository.existsByPhoneNumberAndIdNot(newPhone, user.getId())) {
            throw new DonorProfileException("The phone number '" + newPhone + "' is already in use by another account.");
        }

        // 2. Resolve and validate Province
        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new DonorProfileException("Selected province does not exist."));

        // 3. Resolve and validate City belongs to chosen Province
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new DonorProfileException("Selected city does not exist."));

        if (!city.getProvince().getId().equals(province.getId())) {
            throw new DonorProfileException("The selected city '" + city.getName() +
                    "' does not belong to '" + province.getName() + "'.");
        }

        // 4. Update permitted User fields
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(newPhone);
        userRepository.save(user);

        // 5. Update permitted Donor fields
        donor.setWeight(request.getWeight());
        donor.setProvince(province);
        donor.setCity(city);
        donor.setCompleteAddress(request.getCompleteAddress() != null ? request.getCompleteAddress().trim() : null);

        // Upload new profile photo if provided
        if (request.getPhotoFile() != null && !request.getPhotoFile().isEmpty()) {
            try {
                String storedPhoto = fileUploadService.uploadDonorPhoto(request.getPhotoFile());
                donor.setPhoto(storedPhoto);
            } catch (IllegalArgumentException ex) {
                throw new DonorProfileException(ex.getMessage());
            }
        }

        Donor savedDonor = donorRepository.save(donor);
        log.info("Donor profile updated successfully for user: {}", email);
        return savedDonor;
    }

    /**
     * Updates the operational availability status of the authenticated donor.
     *
     * @param email        Authenticated user email
     * @param availability New availability status
     * @return Updated {@link Donor} entity
     */
    @Transactional
    public Donor updateAvailability(String email, DonorAvailability availability) {
        if (availability == null) {
            throw new DonorProfileException("Availability status must not be null.");
        }
        Donor donor = getDonorByUserEmail(email);
        donor.setAvailability(availability);
        Donor savedDonor = donorRepository.save(donor);
        log.info("Donor availability updated to '{}' for user: {}", availability, email);
        return savedDonor;
    }

    /**
     * Retrieves the complete donation history for the authenticated donor.
     *
     * @param email Authenticated user email
     * @return List of donations ordered by date descending
     */
    @Transactional(readOnly = true)
    public List<Donation> getDonationHistory(String email) {
        Donor donor = getDonorByUserEmail(email);
        return donationRepository.findByDonorIdWithDetailsOrderByDonationDateDesc(donor.getId());
    }

    /**
     * Aggregates real database records to assemble the donor's dashboard summary.
     *
     * @param email Authenticated user email
     * @return Fully populated {@link DonorDashboardSummary}
     */
    @Transactional(readOnly = true)
    public DonorDashboardSummary getDashboardSummary(String email) {
        Donor donor = getDonorByUserEmail(email);
        DonorEligibilityResult eligibility = eligibilityService.evaluateEligibility(donor);
        List<Donation> donations = donationRepository.findByDonorIdWithDetailsOrderByDonationDateDesc(donor.getId());

        long totalDonations = donations.size();
        long completedDonations = donations.stream()
                .filter(d -> d.getDonationStatus() == DonationStatus.COMPLETED)
                .count();

        int totalUnits = donations.stream()
                .filter(d -> d.getDonationStatus() == DonationStatus.COMPLETED)
                .mapToInt(Donation::getUnitsDonated)
                .sum();

        // Calculate profile completion percentage
        int completion = 70; // Basic required fields registered
        if (donor.getWeight() != null) completion += 15;
        if (donor.getCompleteAddress() != null && !donor.getCompleteAddress().isBlank()) completion += 15;

        List<Donation> recent = donations.stream().limit(3).toList();

        long incomingRequests = requestMatchRepository.countByDonorUserEmailAndMatchStatusIn(
                email, List.of(com.donornexa.common.enums.MatchStatus.PENDING, com.donornexa.common.enums.MatchStatus.INVITED));
        long acceptedRequests = requestMatchRepository.countByDonorUserEmailAndMatchStatus(
                email, com.donornexa.common.enums.MatchStatus.ACCEPTED);
        long rejectedRequests = requestMatchRepository.countByDonorUserEmailAndMatchStatus(
                email, com.donornexa.common.enums.MatchStatus.REJECTED);
        long scheduledDonations = requestMatchRepository.countByDonorUserEmailAndMatchStatus(
                email, com.donornexa.common.enums.MatchStatus.SCHEDULED);
        long inProgressDonations = requestMatchRepository.countByDonorUserEmailAndMatchStatusIn(
                email, List.of(com.donornexa.common.enums.MatchStatus.ARRIVED, com.donornexa.common.enums.MatchStatus.DONATION_IN_PROGRESS));

        List<com.donornexa.matching.entity.RequestMatch> appointments = requestMatchRepository
                .findByDonorEmailAndMatchStatusInWithDetails(email,
                        List.of(com.donornexa.common.enums.MatchStatus.SCHEDULED,
                                com.donornexa.common.enums.MatchStatus.ARRIVED,
                                com.donornexa.common.enums.MatchStatus.DONATION_IN_PROGRESS));

        long unreadMessages = messageRepository.countByRecipientIdAndIsReadFalse(donor.getUser().getId());
        List<com.donornexa.notification.entity.Notification> recentNotifications = notificationRepository.findTop5ByRecipientIdOrderByCreatedAtDesc(donor.getUser().getId());

        return DonorDashboardSummary.builder()
                .donor(donor)
                .eligibility(eligibility)
                .totalDonationsCount(totalDonations)
                .completedDonationsCount(completedDonations)
                .totalUnitsDonated(totalUnits)
                .profileCompletionPercentage(completion)
                .recentDonations(recent)
                .incomingRequestsCount(incomingRequests)
                .acceptedRequestsCount(acceptedRequests)
                .rejectedRequestsCount(rejectedRequests)
                .scheduledDonationsCount(scheduledDonations)
                .inProgressDonationsCount(inProgressDonations)
                .upcomingAppointments(appointments)
                .unreadMessagesCount(unreadMessages)
                .recentNotifications(recentNotifications)
                .build();
    }
}
