package com.cinema.ticket_booking.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 64, message = "Имя пользователя слишком длинное")
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    @Size(max = 254, message = "Email адрес слишком длинный")
    private String email;

    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Не верный формат телефона")
    @Column(nullable = false, unique = true)
    private String phone;

    @NotNull
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
