package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {}
