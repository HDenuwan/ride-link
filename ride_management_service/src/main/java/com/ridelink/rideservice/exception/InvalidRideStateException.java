package com.ridelink.rideservice.exception;

public class InvalidRideStateException extends RuntimeException {
    public InvalidRideStateException(String msg) { super(msg); }
}
