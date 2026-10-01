package com.cinema.ticket_booking.DTO;

import java.time.LocalDate;

public record RegisterRequest(
    String name,
    String email,
    String phone,
    LocalDate birthDate,
    String password
) {}
