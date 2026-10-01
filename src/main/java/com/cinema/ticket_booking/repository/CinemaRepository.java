package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Cinema;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {}
