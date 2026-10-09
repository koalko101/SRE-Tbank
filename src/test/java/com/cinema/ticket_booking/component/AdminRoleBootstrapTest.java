package com.cinema.ticket_booking.component;

import com.cinema.ticket_booking.DTO.Role;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminRoleBootstrapTest {
    private final UserRepository users = mock(UserRepository.class);

    @Test
    void configuredAccountIsPromotedToAdmin() {
        User user = new User();
        user.setRole(Role.USER);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        new AdminRoleBootstrap(users, " admin@example.com ").run(null);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(users).save(user);
    }

    @Test
    void emptyConfigurationDoesNotSearchForAnAccount() {
        new AdminRoleBootstrap(users, " ").run(null);

        verify(users, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }
}
