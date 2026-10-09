package com.cinema.ticket_booking.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cinema.ticket_booking.DTO.LoginRequest;
import com.cinema.ticket_booking.DTO.RegisterRequest;
import com.cinema.ticket_booking.DTO.Role;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.DTO.UserSecret;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.UserSecretRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserSecretRepository userSecretRepository;
    private final PasswordEncoder passwordEncoder;

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
            throw new IllegalArgumentException("Заполните имя, email, телефон, дату рождения и пароль (не менее 8 символов)");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Этот email уже зарегистрирован");
        }
        
        if (userRepository.existsByPhone(request.phone())) {
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
        log.info("Account registration completed");

        return user;
    }

    @Transactional
    public User login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("Authentication failed: invalid credentials");
            return new IllegalArgumentException("Неверный email или пароль");
        });
        
        UserSecret secret = userSecretRepository.findPasswordHashByUserId(user.getId()).orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));
       
        if (!passwordEncoder.matches(request.password(), secret.getPasswordHash())) {
            log.warn("Authentication failed: invalid credentials");
            throw new IllegalArgumentException("Неверный email или пароль");
        }

        if (user.getRole() == null) {
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        log.info("Authentication succeeded");
        return user;
    }
}
