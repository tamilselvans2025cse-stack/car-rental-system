package com.example.carrental.exception;

public class BookingException extends CarRentalException {

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message);
    }
}
