package com.cinema.ticket_booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.ticket_booking.DTO.Screening;
import java.time.LocalDateTime;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    Page<Screening> findAllByOrderByStartTime(Pageable pageable);

    Page<Screening> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Screening> findByMovie_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
            Long movieId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Screening> findByHall_Cinema_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
            Long cinemaId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Screening> findByIdIn(List<Long> ids);

    boolean existsByMovie_Id(Long movieId);

}
