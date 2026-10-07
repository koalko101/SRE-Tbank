package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Ticket;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByScreening_Id(Long screeningId);

    List<Ticket> findByUser_EmailOrderByCreatedAtDesc(String email);

    Optional<Ticket> findByIdAndUser_Email(Long id, String email);

    boolean existsByScreening_IdAndSeat_Id(Long screeningId, Long seatId);
}
