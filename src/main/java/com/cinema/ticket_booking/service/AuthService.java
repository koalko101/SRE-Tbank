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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class AuthService {

    private final UserRepository userRepository;
    private final UserSecretRepository userSecretRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public User register(RegisterRequest request) {
        
        User user = new User();
        UserSecret userSecret = new UserSecret();

        user.setName(request.name());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setBirthDate(request.birthDate());
        
        userSecret.setUser(user);
        userSecret.setPasswordHash(passwordEncoder.encode(request.password()));
        
        userRepository.save(user);
        userSecretRepository.save(userSecret);

        return user;

    }


    public User login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        UserSecret secret = userSecretRepository.findPasswordHashByUserId(user.getId()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.password(), secret.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return user;
    }


}
