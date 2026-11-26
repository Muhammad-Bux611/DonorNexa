package com.donornexa.hospital.dto;

import com.donornexa.donation.entity.Donation;
import com.donornexa.notification.entity.Notification;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aggregated metrics and profile details for the hospital dashboard.
 */
@Getter
@Builder
public class HospitalDashboardSummary {

    private Long hospitalId;
    private String hospitalName;
    private String registrationNumber;
    private String email;
    private String contactNumber;
    private String provinceName;
    private String cityName;
    private String completeAddress;
    private Boolean verified;
    private String rejectionReason;
    private HospitalVerificationStatus verificationStatus;
    private AccountStatus accountStatus;
    private boolean profileComplete;
    private LocalDateTime registrationDate;

    // Blood Request & Fulfillment Metrics
    private long totalRequestsCreated;
    private long pendingRequestsCount;
    private long approvedRequestsCount;
    private long activeRequestsCount;
    private long partiallyFulfilledRequestsCount;
    private long fulfilledRequestsCount;
    private long totalUnitsRequired;
    private long confirmedUnitsFulfilled;
    private long remainingUnitsRequired;

    // Donor Assignment & Coordination Metrics
    private long matchedDonorsCount;
    private long acceptedDonorsCount;
    private long scheduledDonorsCount;
    private long arrivedDonorsCount;
    private List<RequestMatch> activeAssignments;

    // Donation & Blood Inventory Metrics
    private long totalDonationsCount;
    private long completedDonationsCount;
    private long totalAvailableStock;
    private long nearExpiryStock;
    private long expiredStock;
    private Map<BloodGroup, Long> unitsByGroup;
    private List<Donation> recentDonations;

    private long unreadMessagesCount;
    private List<Notification> recentNotifications;
}
