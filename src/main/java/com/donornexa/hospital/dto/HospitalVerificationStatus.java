package com.donornexa.hospital.dto;

/**
 * High-level verification states for hospital profiles.
 * Derived dynamically from {@code verified} and {@code rejectionReason}.
 */
public enum HospitalVerificationStatus {
    PENDING("Pending Verification", "badge bg-warning text-dark"),
    VERIFIED("Verified", "badge bg-success"),
    REJECTED("Rejected", "badge bg-danger");

    private final String displayName;
    private final String badgeClass;

    HospitalVerificationStatus(String displayName, String badgeClass) {
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
