package com.cinema.ticket_booking.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    
    @NotBlank(message = "Email не должен быть пустым")
    @Email(message = "Не верный email")
    String email,
    
    @Size(min = 8, max = 255, message = "Пароль должен быть больше 8 и меньше 255 символов")
    String password) {}
