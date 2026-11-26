package com.donornexa.hospital.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Analytical summary DTO for the hospital reports module.
 */
@Getter
@Builder
public class HospitalReportSummaryDTO {

    private final String hospitalName;
    private final String registrationNumber;

    // Blood Request Metrics
    private final long totalRequests;
    private final long pendingRequests;
    private final long matchingRequests;
    private final long fulfilledRequests;
    private final long cancelledRequests;

    // Donor Matching Metrics
    private final long totalMatches;
    private final long acceptedMatches;
    private final long declinedMatches;
    private final long pendingMatches;

    // Donation Metrics
    private final long totalDonations;
    private final long completedDonations;
    private final long scheduledDonations;
    private final long totalUnitsCollected;

    // Monthly Analytics (Chronological Map)
    private final Map<String, Long> monthlyRequests;
    private final Map<String, Long> monthlyDonations;
}
