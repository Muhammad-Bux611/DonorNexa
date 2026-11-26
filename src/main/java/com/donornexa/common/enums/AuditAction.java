package com.donornexa.common.enums;

/**
 * Represents the type of action recorded in the audit log.
 * Stored as a STRING in the database.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    VERIFY,
    STATUS_CHANGE,
    REVEAL_CONTACT,
    EXPIRE_REQUEST,
    DONATION_RECORDED,
    INVENTORY_UPDATE,
    TRUST_SCORE_UPDATE
}
