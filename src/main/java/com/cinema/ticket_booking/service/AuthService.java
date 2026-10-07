package com.cinema.ticket_booking.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cinema.ticket_booking.DTO.LoginRequest;
import com.cinema.ticket_booking.DTO.RegisterRequest;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.DTO.UserSecret;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.UserSecretRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSecretRepository userSecretRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.phone() == null || request.phone().isBlank()
                || request.birthDate() == null
                || request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Заполните имя, email, телефон, дату рождения и пароль (не менее 6 символов)");
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

        user.setName(request.name());
        user.setPhone(request.phone());
        user.setEmail(email);
        user.setBirthDate(request.birthDate());
        userSecret.setUser(user);
        userSecret.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);
        userSecretRepository.save(userSecret);

        return user;
    }

    public User login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));
        
        UserSecret secret = userSecretRepository.findPasswordHashByUserId(user.getId()).orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));
       
        if (!passwordEncoder.matches(request.password(), secret.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный email или пароль");
        }

        return user;
    }
}
