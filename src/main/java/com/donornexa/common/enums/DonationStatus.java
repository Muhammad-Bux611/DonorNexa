package com.donornexa.common.enums;

/**
 * Represents the status of a blood donation event.
 * Stored as a STRING in the database.
 */
public enum DonationStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    REJECTED
}
