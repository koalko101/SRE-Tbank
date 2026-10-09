package com.cinema.ticket_booking.component;

import com.cinema.ticket_booking.DTO.Role;
import com.cinema.ticket_booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminRoleBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final String adminEmail;

    public AdminRoleBootstrap(UserRepository users,
            @Value("${app.admin.email:}") String adminEmail) {
        this.users = users;
        this.adminEmail = adminEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()) {
            return;
        }
        users.findByEmail(adminEmail.trim()).ifPresent(user -> {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                users.save(user);
            }
        });
    }
}
