package com.donornexa.donor.dto;

import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.notification.entity.Notification;
import com.donornexa.matching.entity.RequestMatch;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Summary view model providing real database statistics and records
 * for the Donor Dashboard.
 */
@Getter
@Builder
public class DonorDashboardSummary {

    private final Donor donor;
    private final DonorEligibilityResult eligibility;
    private final long totalDonationsCount;
    private final long completedDonationsCount;
    private final int totalUnitsDonated;
    private final int profileCompletionPercentage;
    private final List<Donation> recentDonations;

    // Request & Match Assignment Metrics
    private final long incomingRequestsCount;
    private final long acceptedRequestsCount;
    private final long rejectedRequestsCount;
    private final long scheduledDonationsCount;
    private final long inProgressDonationsCount;
    private final List<RequestMatch> upcomingAppointments;

    private final long unreadMessagesCount;
    private final List<Notification> recentNotifications;
}
