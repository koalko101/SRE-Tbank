package com.cinema.ticket_booking.security;

import com.cinema.ticket_booking.DTO.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService("test-secret-key-that-is-long-enough-for-hmac-sha256", 60_000);

    @Test
    void generatedTokenContainsUserEmail() {
        String token = jwtService.generate("alex@example.com", Role.USER);

        assertThat(jwtService.subject(token)).isEqualTo("alex@example.com");
        assertThat(jwtService.role(token)).isEqualTo(Role.USER);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generate("alex@example.com", Role.ADMIN);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.subject(tampered)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void generatedTokenContainsAdminRoleClaim() {
        String token = jwtService.generate("admin@example.com", Role.ADMIN);

        assertThat(jwtService.role(token)).isEqualTo(Role.ADMIN);
    }
}
