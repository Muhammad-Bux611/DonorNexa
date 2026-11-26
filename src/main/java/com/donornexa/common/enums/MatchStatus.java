package com.donornexa.common.enums;

/**
 * Represents the status of a donor-to-request match and coordination assignment lifecycle.
 * Stored as a STRING in the database.
 */
public enum MatchStatus {
    INVITED,
    PENDING, // Backwards-compatible alias for INVITED
    ACCEPTED,
    REJECTED,
    SCHEDULED,
    ARRIVED,
    DONATION_IN_PROGRESS,
    DONATED,
    CONFIRMED,
    COMPLETED, // Backwards-compatible alias for CONFIRMED
    CANCELLED,
    NO_SHOW,
    EXPIRED,
    NO_RESPONSE;

    public boolean isPendingResponse() {
        return this == PENDING || this == INVITED;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED || this == COMPLETED;
    }

    public boolean isActiveAssignment() {
        return this == ACCEPTED || this == SCHEDULED || this == ARRIVED || this == DONATION_IN_PROGRESS;
    }
}
