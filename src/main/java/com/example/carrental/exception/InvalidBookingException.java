package com.example.carrental.exception;

/**
 * Thrown when booking validation, status transitions, or inputs are invalid.
 */
public class InvalidBookingException extends CarRentalException {
    public InvalidBookingException(String message) {
        super(message);
    }
}

