package com.cinema.ticket_booking.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя обязательно")
    @Column(nullable = false)
    @Max(value = 64, message = "Имя пользователя слишком длинное")
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    @Max(value = 254, message = "Email адрес слишком длинный")
    private String email;

    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Не верный формат телефона")
    @Column(nullable = false, unique = true)
    private String phone;

    @NotNull
    @Column(name = "birth_date", nullable = false, columnDefinition = "DATE CHECK (birth_date >= 1900-01-01 AND birth_date <= CURRENT_DATE)")
    private LocalDate birthDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
