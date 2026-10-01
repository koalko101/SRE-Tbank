package com.cinema.ticket_booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Screening;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    Page<Screening> findScreeningsOrderByStartTime(Pageable pageable);

}
