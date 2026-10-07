package com.cinema.ticket_booking.controller;

public class AgeRestrictionException extends RuntimeException {
    public AgeRestrictionException(String message) {
        super(message);
    }
}
