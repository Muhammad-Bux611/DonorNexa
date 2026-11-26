package com.donornexa.common.exception;

/**
 * Thrown when a registration attempt violates a business rule
 * (e.g. duplicate email, duplicate phone, password mismatch).
 * Controllers catch this and display a user-friendly message.
 */
public class RegistrationException extends RuntimeException {

    public RegistrationException(String message) {
        super(message);
    }
}
