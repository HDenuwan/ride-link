package com.ridelink.accountservice.exception;

/**
 * Thrown when attempting to register with duplicate email or phone.
 */
public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException(String message) {
        super(message);
    }
}
