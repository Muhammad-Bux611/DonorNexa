package com.donornexa.donor.dto;

/**
 * High-level verification states for donor profiles.
 * Derived dynamically from {@code verified} and {@code rejectionReason}.
 */
public enum DonorVerificationStatus {
    PENDING("Pending Verification", "badge bg-warning text-dark"),
    VERIFIED("Verified", "badge bg-success"),
    REJECTED("Rejected", "badge bg-danger");

    private final String displayName;
    private final String badgeClass;

    DonorVerificationStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
