package com.donornexa.matching.dto;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestUrgency;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Summary of a matching run for a blood request, including evaluation metrics,
 * exclusion reasons, and ranked donor matches.
 */
@Getter
@Builder
public class MatchingEvaluationSummaryDTO {

    private Long bloodRequestId;
    private BloodGroup requiredBloodGroup;
    private RequestUrgency urgency;
    private String hospitalName;
    private String hospitalCity;
    private String hospitalProvince;

    private int totalDonorsEvaluated;
    private int excludedIncompatible;
    private int excludedUnverified;
    private int excludedUnavailable;
    private int excludedMedicalEligibility;
    private int excludedAccountInactive;
    private int totalMatchesFound;

    private List<DonorMatchDTO> matches;
    private String message;
}
