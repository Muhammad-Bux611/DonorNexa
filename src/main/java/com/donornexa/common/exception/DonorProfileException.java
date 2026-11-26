package com.donornexa.common.exception;

/**
 * Exception thrown when a donor profile operation violates business rules.
 */
public class DonorProfileException extends RuntimeException {

    public DonorProfileException(String message) {
        super(message);
    }
}
