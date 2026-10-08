package com.example.carrental.exception;

/**
 * Thrown when unauthorized access is attempted on a booking or admin area.
 */
public class UnauthorizedException extends CarRentalException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

