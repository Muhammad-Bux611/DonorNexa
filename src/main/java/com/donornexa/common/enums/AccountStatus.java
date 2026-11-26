package com.donornexa.common.enums;

/**
 * Represents the lifecycle status of a user account.
 * Stored as a STRING in the database.
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}
