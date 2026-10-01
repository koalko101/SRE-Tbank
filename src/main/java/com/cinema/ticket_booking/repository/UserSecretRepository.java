package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.UserSecret;

import java.util.Optional;


public interface UserSecretRepository extends JpaRepository<UserSecret, Long> {

    Optional<UserSecret> findPasswordHashByUserId(Long userId);

}
