package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {}
