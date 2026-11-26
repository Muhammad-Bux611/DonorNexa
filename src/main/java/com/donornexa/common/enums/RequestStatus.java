package com.donornexa.common.enums;

/**
 * Represents the lifecycle status of a blood request.
 * Stored as a STRING in the database.
 */
public enum RequestStatus {
    PENDING_ADMIN_APPROVAL,
    PENDING, // Backwards-compatible alias for PENDING_ADMIN_APPROVAL
    APPROVED,
    MATCHING_IN_PROGRESS,
    MATCHING, // Backwards-compatible alias for MATCHING_IN_PROGRESS
    PARTIALLY_FULFILLED,
    FULFILLED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    public boolean isPendingApproval() {
        return this == PENDING_ADMIN_APPROVAL || this == PENDING;
    }

    public boolean isApprovedOrActive() {
        return this == APPROVED || this == MATCHING || this == MATCHING_IN_PROGRESS || this == PARTIALLY_FULFILLED;
    }

    public boolean isFulfilled() {
        return this == FULFILLED;
    }
}
