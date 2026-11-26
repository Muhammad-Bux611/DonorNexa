package com.donornexa.common.enums;

/**
 * Categorises the type of a notification sent to users.
 * Stored as a STRING in the database.
 */
public enum NotificationType {
    BLOOD_REQUEST,
    DONOR_MATCH,
    DONATION_REMINDER,
    DONATION_UPDATE,
    SYSTEM_ALERT,
    MESSAGE
}
