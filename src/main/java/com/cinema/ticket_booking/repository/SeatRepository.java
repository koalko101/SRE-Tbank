package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Seat;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByHall_IdOrderByRowNumberAscSeatNumberAsc(Long hallId);

    List<Seat> findByIdIn(List<Long> ids);
}
