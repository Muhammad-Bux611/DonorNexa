package com.donornexa.common.exception;

/**
 * Thrown when a hospital domain operation violates business, ownership,
 * or relational rules.
 */
public class HospitalOperationException extends RuntimeException {

    public HospitalOperationException(String message) {
        super(message);
    }
}
