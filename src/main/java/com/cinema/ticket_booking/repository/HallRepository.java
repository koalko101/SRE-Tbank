package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Hall;

public interface HallRepository extends JpaRepository<Hall, Long>{}
