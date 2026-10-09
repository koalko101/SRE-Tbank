package com.cinema.ticket_booking.DTO;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 2, max = 64)
        String name,
        @NotBlank
        @Email
        @Size(max = 254)
        String email,
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$")
        String phone,
        @NotNull
        @PastOrPresent
        LocalDate birthDate,
        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {}
