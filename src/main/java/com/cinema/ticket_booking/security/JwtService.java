package com.cinema.ticket_booking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.cinema.ticket_booking.DTO.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${security.jwt.secret}") String secret, @Value("${security.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(String email, Role role) {
        Date now = new Date();
        return Jwts.builder()
                   .subject(email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }
    
    public String subject(String token) {
        return parse(token).getSubject();
    }

    public Role role(String token) {
        String role = parse(token).get("role", String.class);
        return Role.valueOf(role.toUpperCase(Locale.ROOT));
    }

    private io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
