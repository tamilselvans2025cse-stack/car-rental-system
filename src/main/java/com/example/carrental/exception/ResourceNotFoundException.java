package com.example.carrental.exception;

/**
 * Thrown when a requested vehicle, user, or reservation is not found.
 */
public class ResourceNotFoundException extends CarRentalException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

