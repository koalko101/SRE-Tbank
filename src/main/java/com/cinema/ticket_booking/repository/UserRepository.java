package com.cinema.ticket_booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.User;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

}
