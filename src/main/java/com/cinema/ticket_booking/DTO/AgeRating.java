package com.cinema.ticket_booking.DTO;

public enum AgeRating {
    R0(0),
    R6(6),
    R12(12),
    R16(16),
    R18(18);

    private final int minimumAge;

    AgeRating(int minimumAge) {
        this.minimumAge = minimumAge;
    }

    public int getMinimumAge() {
        return minimumAge;
    }
}
