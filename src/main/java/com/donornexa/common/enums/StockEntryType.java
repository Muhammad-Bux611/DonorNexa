package com.donornexa.common.enums;

/**
 * Type of blood stock intake, dispatch, or adjustment event.
 */
public enum StockEntryType {
    DONATION_INTAKE,
    DIRECT_INTAKE,
    REQUEST_DISPATCH,
    DISCARD_EXPIRED,
    ADJUSTMENT
}
