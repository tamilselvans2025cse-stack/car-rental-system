package com.example.carrental.exception;

/**
 * Base custom runtime exception for car rental business rule violations.
 */
public class CarRentalException extends RuntimeException {
    public CarRentalException(String message) {
        super(message);
    }
}

