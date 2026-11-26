package com.donornexa.donor.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Encapsulates the complete medical and operational eligibility evaluation
 * for a blood donor based on approved university CEP rules.
 */
@Getter
@Builder
public class DonorEligibilityResult {

    /** True if the donor meets all criteria to donate blood today. */
    private final boolean eligible;

    /** Calculated current age in years. */
    private final int age;

    /** Age criterion satisfied (18 - 60 years inclusive). */
    private final boolean ageEligible;

    /** Weight criterion satisfied (>= 50.0 kg). */
    private final boolean weightEligible;

    /** Cooldown interval criterion satisfied (>= 90 days since last donation). */
    private final boolean cooldownEligible;

    /** Operational availability criterion satisfied (status == AVAILABLE). */
    private final boolean availabilityEligible;

    /** Account status criterion satisfied (account not suspended or inactive). */
    private final boolean accountEligible;

    /** Number of days elapsed since the donor's last donation, or null if first-time donor. */
    private final Long daysSinceLastDonation;

    /** Number of cooldown days remaining before the donor can donate again, or 0 if cooldown satisfied. */
    private final Long daysRemainingCooldown;

    /** Estimated next eligible donation date, or null if already eligible. */
    private final LocalDate nextEligibleDate;

    /** Detailed explanation messages for each evaluation criterion. */
    private final List<String> criteriaMessages;

    /** High-level headline / summary of eligibility. */
    private final String summaryMessage;

    /** CSS class for UI badges (e.g., bg-success, bg-warning, bg-danger). */
    private final String statusBadgeClass;
}
