package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.DTO.LoginRequest;
import com.cinema.ticket_booking.DTO.RegisterRequest;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.security.JwtService;
import com.cinema.ticket_booking.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        return response(authService.register(request));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        return response(authService.login(request));
    }

    private Map<String, Object> response(User user) {
        return Map.of(
                "token", jwtService.generate(user.getEmail()),
                "user", Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail()));
    }
}
