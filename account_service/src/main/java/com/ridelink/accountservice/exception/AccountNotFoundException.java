package com.ridelink.accountservice.exception;

/**
 * Thrown when a requested account is not found.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
