package com.donornexa.common.exception;

/**
 * Thrown when an expected Hospital profile cannot be located in the database.
 */
public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(String message) {
        super(message);
    }
}
