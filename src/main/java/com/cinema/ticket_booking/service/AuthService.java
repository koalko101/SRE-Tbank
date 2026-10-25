package com.cinema.ticket_booking.service;

import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cinema.ticket_booking.DTO.LoginRequest;
import com.cinema.ticket_booking.DTO.RegisterRequest;
import com.cinema.ticket_booking.DTO.Role;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.DTO.UserSecret;
import com.cinema.ticket_booking.component.BusinessMetrics;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.UserSecretRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSecretRepository userSecretRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessMetrics metrics;


    @Transactional
    public User register(RegisterRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.phone() == null || request.phone().isBlank()
                || request.birthDate() == null
                || request.birthDate().isAfter(LocalDate.now())
                || request.birthDate().isBefore(LocalDate.now().minusYears(120))
                || request.name().trim().length() > 64
                || request.password() == null || request.password().length() < 8) {

            
            log.atWarn()
               .addKeyValue("event", "auth.register.failure")
               .addKeyValue("reason", "invalid_credentials")
               .log("Invalid credentials");
            
            metrics.failureRegistered();

            throw new IllegalArgumentException("Заполните имя, email, телефон, дату рождения и пароль (не менее 8 символов)");
        }
        String email = request.email().trim().toLowerCase();
        
        if (userRepository.findByEmail(email).isPresent()) {
            log.atWarn()
               .addKeyValue("event", "auth.register.failure")
               .addKeyValue("reason", "user_exists")
               .addKeyValue("type", "email")
               .log("User with the same email was already exists");
            
            metrics.failureRegistered();

            throw new IllegalArgumentException("Этот email уже зарегистрирован");
        }
        
        if (userRepository.existsByPhone(request.phone())) {
            log.atWarn()
               .addKeyValue("event", "auth.register.failure")
               .addKeyValue("reason", "user_exists")
               .addKeyValue("type", "phone")
               .log("User with the same phone was already exists");
            
            metrics.failureRegistered();

            throw new IllegalArgumentException("Этот номер телефона уже зарегистрирован");
        }

        User user = new User();
        UserSecret userSecret = new UserSecret();

        user.setName(request.name().trim());
        user.setPhone(request.phone());
        user.setEmail(email);
        user.setBirthDate(request.birthDate());
        user.setRole(Role.USER);
        userSecret.setUser(user);
        userSecret.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);
        userSecretRepository.save(userSecret);
        log.atInfo()
           .addKeyValue("event", "auth.registration.success")
           .addKeyValue("reason", "success")
           .log("Account registration completed");

        metrics.successRegistered();

        return user;
    }

    @Transactional
    public User login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.atWarn()
               .addKeyValue("event", "auth.login.failure")
               .addKeyValue("reason", "invalid_credentials")
               .log("Invalid credentials");
            metrics.failureAuth();

            return new IllegalArgumentException("Неверный email или пароль");
        });
        
        UserSecret secret = userSecretRepository.findPasswordHashByUserId(user.getId()).orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));
       
        if (!passwordEncoder.matches(request.password(), secret.getPasswordHash())) {
            log.atWarn()
               .addKeyValue("event", "auth.login.failure")
               .addKeyValue("reason", "incorrect_password")
               .log("Incorrect password");
            metrics.failureAuth();

            throw new IllegalArgumentException("Неверный email или пароль");
        }

        if (user.getRole() == null) {
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        log.atInfo()
           .addKeyValue("event", "auth.login.success")
           .addKeyValue("reason", "success")
           .log("Authentication succeeded");
        metrics.successAuth();

        return user;
    }
}
