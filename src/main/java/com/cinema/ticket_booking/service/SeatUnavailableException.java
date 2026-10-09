package com.cinema.ticket_booking.service;

public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException() {
        super("Одно или несколько мест уже заняты. Обновите схему зала и выберите другие места.");
    }

    public SeatUnavailableException(Throwable cause) {
        super("Одно или несколько мест уже заняты. Обновите схему зала и выберите другие места.", cause);
    }
}
