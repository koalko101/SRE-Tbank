package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {}
