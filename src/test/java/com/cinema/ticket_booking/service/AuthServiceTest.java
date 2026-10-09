package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.DTO.LoginRequest;
import com.cinema.ticket_booking.DTO.RegisterRequest;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.DTO.UserSecret;
import com.cinema.ticket_booking.repository.UserRepository;
import com.cinema.ticket_booking.repository.UserSecretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock UserSecretRepository userSecretRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    void registerStoresProfileAndPasswordHash() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        RegisterRequest request = new RegisterRequest("Алексей", "alex@example.com", "+375291112233", LocalDate.of(2000, 1, 1), "secret123");

        User result = authService.register(request);

        assertThat(result.getName()).isEqualTo("Алексей");
        assertThat(result.getEmail()).isEqualTo("alex@example.com");
        assertThat(result.getRole()).isEqualTo(com.cinema.ticket_booking.DTO.Role.USER);
        verify(userRepository).save(any(User.class));
        verify(userSecretRepository).save(argThat(secret -> "hashed-secret".equals(secret.getPasswordHash())));
    }

    @Test
    void registerRejectsFutureBirthDate() {
        RegisterRequest request = new RegisterRequest(
                "Алексей", "alex@example.com", "+375291112233", LocalDate.now().plusDays(1), "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userRepository, userSecretRepository, passwordEncoder);
    }

    @Test
    void registerRejectsBirthDateOlderThan120Years() {
        RegisterRequest request = new RegisterRequest(
                "Alex", "alex@example.com", "+375291112233", LocalDate.of(200, 1, 1), "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userRepository, userSecretRepository, passwordEncoder);
    }

    @Test
    void registerRejectsNameLongerThan64Characters() {
        RegisterRequest request = new RegisterRequest(
                "А".repeat(65), "alex@example.com", "+375291112233", LocalDate.of(2000, 1, 1), "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userRepository, userSecretRepository, passwordEncoder);
    }

    @Test
    void loginReturnsUserForValidPassword() {
        User user = new User(); user.setId(7L); user.setEmail("alex@example.com");
        UserSecret secret = new UserSecret(); secret.setPasswordHash("hashed-secret");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userSecretRepository.findPasswordHashByUserId(7L)).thenReturn(Optional.of(secret));
        when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);

        assertThat(authService.login(new LoginRequest(user.getEmail(), "secret"))).isSameAs(user);
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = new User(); user.setId(7L); user.setEmail("alex@example.com");
        UserSecret secret = new UserSecret(); secret.setPasswordHash("hashed-secret");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userSecretRepository.findPasswordHashByUserId(7L)).thenReturn(Optional.of(secret));
        when(passwordEncoder.matches("wrong", "hashed-secret")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Неверный email или пароль");
    }
}
