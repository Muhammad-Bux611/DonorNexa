package com.donornexa.hospital.service;

import com.donornexa.location.entity.City;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.hospital.dto.HospitalDashboardSummary;
import com.donornexa.hospital.dto.HospitalProfileDTO;
import com.donornexa.hospital.dto.HospitalProfileUpdateDTO;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing hospital profiles, dashboard metrics, and permitted profile updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final com.donornexa.donation.repository.DonationRepository donationRepository;
    private final com.donornexa.inventory.repository.BloodStockEntryRepository bloodStockEntryRepository;
    private final com.donornexa.bloodrequest.repository.BloodRequestRepository bloodRequestRepository;
    private final com.donornexa.matching.repository.RequestMatchRepository requestMatchRepository;
    private final com.donornexa.notification.repository.NotificationRepository notificationRepository;
    private final com.donornexa.messaging.repository.MessageRepository messageRepository;

    /**
     * Retrieve the hospital associated with the authenticated user's email.
     *
     * @param email Authenticated user email
     * @return Fully populated {@link Hospital} entity
     * @throws HospitalNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Hospital getHospitalByEmail(String email) {
        return hospitalRepository.findByUserEmailWithDetails(email.toLowerCase().trim())
                .orElseThrow(() -> new HospitalNotFoundException(
                        "No hospital profile found for authenticated user: " + email));
    }

    /**
     * Retrieve a hospital profile by its primary key ID.
     *
     * @param id Hospital primary key ID
     * @return Fully populated {@link Hospital} entity
     * @throws HospitalNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new HospitalNotFoundException(
                        "No hospital profile found with ID: " + id));
    }

    /**
     * Convert the authenticated user's hospital entity to a read-only DTO.
     */
    @Transactional(readOnly = true)
    public HospitalProfileDTO getHospitalProfile(String email) {
        Hospital hospital = getHospitalByEmail(email);
        return mapToProfileDTO(hospital);
    }

    /**
     * Convert any hospital entity to a read-only DTO by hospital ID (for admin inspection).
     */
    @Transactional(readOnly = true)
    public HospitalProfileDTO getHospitalProfileById(Long id) {
        Hospital hospital = getHospitalById(id);
        return mapToProfileDTO(hospital);
    }

    /**
     * Build aggregated real-time dashboard data for the authenticated hospital.
     */
    @Transactional(readOnly = true)
    public HospitalDashboardSummary getDashboardSummary(String email) {
        Hospital hospital = getHospitalByEmail(email);

        boolean profileComplete = hospital.getHospitalName() != null && !hospital.getHospitalName().isBlank()
                && hospital.getContactNumber() != null && !hospital.getContactNumber().isBlank()
                && hospital.getProvince() != null
                && hospital.getCity() != null
                && hospital.getCompleteAddress() != null && !hospital.getCompleteAddress().isBlank();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate nearExpiryThreshold = today.plusDays(7);

        long totalDonations = donationRepository.countByHospitalId(hospital.getId());
        long completedDonations = donationRepository.countByHospitalIdAndDonationStatus(
                hospital.getId(), com.donornexa.common.enums.DonationStatus.COMPLETED);

        long totalRequests = bloodRequestRepository.countByHospitalId(hospital.getId());
        long pendingRequests = bloodRequestRepository.countByHospitalIdAndStatusIn(hospital.getId(),
                java.util.List.of(com.donornexa.common.enums.RequestStatus.PENDING, com.donornexa.common.enums.RequestStatus.PENDING_ADMIN_APPROVAL));
        long approvedRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospital.getId(), com.donornexa.common.enums.RequestStatus.APPROVED);
        long activeRequests = bloodRequestRepository.countByHospitalIdAndStatusIn(hospital.getId(),
                java.util.List.of(com.donornexa.common.enums.RequestStatus.PENDING, com.donornexa.common.enums.RequestStatus.PENDING_ADMIN_APPROVAL,
                        com.donornexa.common.enums.RequestStatus.APPROVED, com.donornexa.common.enums.RequestStatus.MATCHING,
                        com.donornexa.common.enums.RequestStatus.MATCHING_IN_PROGRESS, com.donornexa.common.enums.RequestStatus.PARTIALLY_FULFILLED));
        long partiallyFulfilledRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospital.getId(), com.donornexa.common.enums.RequestStatus.PARTIALLY_FULFILLED);
        long completedRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospital.getId(), com.donornexa.common.enums.RequestStatus.FULFILLED);

        Long totalReqUnits = bloodRequestRepository.sumUnitsRequiredByHospitalId(hospital.getId());
        long totalUnitsRequired = totalReqUnits != null ? totalReqUnits : 0L;
        Long confFulfilled = bloodRequestRepository.sumUnitsFulfilledByHospitalId(hospital.getId());
        long confirmedUnitsFulfilled = confFulfilled != null ? confFulfilled : 0L;
        long remainingUnitsRequired = Math.max(0, totalUnitsRequired - confirmedUnitsFulfilled);

        long matchedDonors = requestMatchRepository.countByBloodRequestHospitalId(hospital.getId());
        long acceptedDonors = requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospital.getId(), com.donornexa.common.enums.MatchStatus.ACCEPTED);
        long scheduledDonors = requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospital.getId(), com.donornexa.common.enums.MatchStatus.SCHEDULED);
        long arrivedDonors = requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospital.getId(), com.donornexa.common.enums.MatchStatus.ARRIVED)
                + requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospital.getId(), com.donornexa.common.enums.MatchStatus.DONATION_IN_PROGRESS);

        java.util.List<com.donornexa.matching.entity.RequestMatch> activeAssignments = requestMatchRepository.findByHospitalIdAndMatchStatusInWithDetails(
                hospital.getId(),
                java.util.List.of(
                        com.donornexa.common.enums.MatchStatus.INVITED,
                        com.donornexa.common.enums.MatchStatus.PENDING,
                        com.donornexa.common.enums.MatchStatus.ACCEPTED,
                        com.donornexa.common.enums.MatchStatus.SCHEDULED,
                        com.donornexa.common.enums.MatchStatus.ARRIVED,
                        com.donornexa.common.enums.MatchStatus.DONATION_IN_PROGRESS
                )
        );

        long unreadMessagesCount = messageRepository.countByRecipientIdAndIsReadFalse(hospital.getUser().getId());
        java.util.List<com.donornexa.notification.entity.Notification> recentNotifications = notificationRepository.findTop5ByRecipientIdOrderByCreatedAtDesc(hospital.getUser().getId());

        long availableStock = bloodStockEntryRepository.sumTotalAvailableUnitsByHospital(hospital.getId(), today);
        long nearExpiryStock = bloodStockEntryRepository.sumNearExpiryUnitsByHospital(hospital.getId(), today, nearExpiryThreshold);
        long expiredStock = bloodStockEntryRepository.sumExpiredUnitsByHospital(hospital.getId(), today);

        java.util.Map<com.donornexa.common.enums.BloodGroup, Long> unitsByGroup = new java.util.LinkedHashMap<>();
        for (com.donornexa.common.enums.BloodGroup bg : com.donornexa.common.enums.BloodGroup.values()) {
            unitsByGroup.put(bg, bloodStockEntryRepository.sumAvailableUnitsByHospitalAndGroup(hospital.getId(), bg, today));
        }

        java.util.List<com.donornexa.donation.entity.Donation> recentDonations = donationRepository.findRecentByHospitalId(
                hospital.getId(), org.springframework.data.domain.PageRequest.of(0, 5));

        return HospitalDashboardSummary.builder()
                .hospitalId(hospital.getId())
                .hospitalName(hospital.getHospitalName())
                .registrationNumber(hospital.getRegistrationNumber())
                .email(hospital.getUser().getEmail())
                .contactNumber(hospital.getContactNumber())
                .provinceName(hospital.getProvince().getName())
                .cityName(hospital.getCity().getName())
                .completeAddress(hospital.getCompleteAddress())
                .verified(hospital.getVerified())
                .rejectionReason(hospital.getRejectionReason())
                .verificationStatus(hospital.getVerificationStatus())
                .accountStatus(hospital.getUser().getAccountStatus())
                .profileComplete(profileComplete)
                .registrationDate(hospital.getCreatedAt())
                .totalDonationsCount(totalDonations)
                .completedDonationsCount(completedDonations)
                .totalAvailableStock(availableStock)
                .nearExpiryStock(nearExpiryStock)
                .expiredStock(expiredStock)
                .unitsByGroup(unitsByGroup)
                .recentDonations(recentDonations)
                .totalRequestsCreated(totalRequests)
                .pendingRequestsCount(pendingRequests)
                .approvedRequestsCount(approvedRequests)
                .activeRequestsCount(activeRequests)
                .partiallyFulfilledRequestsCount(partiallyFulfilledRequests)
                .fulfilledRequestsCount(completedRequests)
                .totalUnitsRequired(totalUnitsRequired)
                .confirmedUnitsFulfilled(confirmedUnitsFulfilled)
                .remainingUnitsRequired(remainingUnitsRequired)
                .matchedDonorsCount(matchedDonors)
                .acceptedDonorsCount(acceptedDonors)
                .scheduledDonorsCount(scheduledDonors)
                .arrivedDonorsCount(arrivedDonors)
                .activeAssignments(activeAssignments)
                .unreadMessagesCount(unreadMessagesCount)
                .recentNotifications(recentNotifications)
                .build();
    }

    /**
     * Update permitted profile fields for the authenticated hospital.
     * Strictly protects immutable attributes (registrationNumber, verified, accountStatus, role).
     *
     * @param email Authenticated user email
     * @param dto   Validated profile update request
     * @return Updated {@link HospitalProfileDTO}
     */
    @Transactional
    public HospitalProfileDTO updateHospitalProfile(String email, HospitalProfileUpdateDTO dto) {
        Hospital hospital = getHospitalByEmail(email);
        User user = hospital.getUser();

        // Validate province existence
        Province province = provinceRepository.findById(dto.getProvinceId())
                .orElseThrow(() -> new HospitalOperationException("Selected province does not exist"));

        // Validate city existence and relationship to province
        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new HospitalOperationException("Selected city does not exist"));

        if (!city.getProvince().getId().equals(province.getId())) {
            throw new HospitalOperationException(
                    "Selected city '" + city.getName() + "' does not belong to province '" + province.getName() + "'.");
        }

        // Validate contact phone uniqueness if modified
        String newContactNumber = dto.getContactNumber().trim();
        if (!newContactNumber.equalsIgnoreCase(hospital.getContactNumber())) {
            if (userRepository.existsByPhoneNumberAndIdNot(newContactNumber, user.getId())) {
                throw new HospitalOperationException(
                        "Phone number '" + newContactNumber + "' is already in use by another account.");
            }
        }

        // Apply permitted mutations only
        hospital.setHospitalName(dto.getHospitalName().trim());
        hospital.setContactNumber(newContactNumber);
        hospital.setProvince(province);
        hospital.setCity(city);
        hospital.setCompleteAddress(dto.getCompleteAddress() != null ? dto.getCompleteAddress().trim() : null);

        // Keep user synchronization
        user.setFullName(dto.getHospitalName().trim());
        user.setPhoneNumber(newContactNumber);

        hospitalRepository.save(hospital);
        userRepository.save(user);

        log.info("Hospital profile updated successfully for user: {}", email);
        return mapToProfileDTO(hospital);
    }

    private HospitalProfileDTO mapToProfileDTO(Hospital hospital) {
        return HospitalProfileDTO.builder()
                .id(hospital.getId())
                .userId(hospital.getUser().getId())
                .hospitalName(hospital.getHospitalName())
                .registrationNumber(hospital.getRegistrationNumber())
                .email(hospital.getUser().getEmail())
                .contactNumber(hospital.getContactNumber())
                .provinceId(hospital.getProvince().getId())
                .provinceName(hospital.getProvince().getName())
                .cityId(hospital.getCity().getId())
                .cityName(hospital.getCity().getName())
                .completeAddress(hospital.getCompleteAddress())
                .licenceFile(hospital.getLicenceFile())
                .verified(hospital.getVerified())
                .rejectionReason(hospital.getRejectionReason())
                .verificationStatus(hospital.getVerificationStatus())
                .accountStatus(hospital.getUser().getAccountStatus())
                .enabled(hospital.getUser().getEnabled())
                .createdAt(hospital.getCreatedAt())
                .updatedAt(hospital.getUpdatedAt())
                .build();
    }
}
