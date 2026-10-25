package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.DTO.Seat;
import com.cinema.ticket_booking.DTO.Ticket;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.component.BusinessMetrics;
import com.cinema.ticket_booking.controller.AgeRestrictionException;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPurchaseService {

    private final TicketRepository tickets;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    private final UserRepository users;
    private final BusinessMetrics metrics;

    @Transactional
    public List<Ticket> purchase(Long screeningId, List<Long> seatIds, String email) {
        metrics.attemptBooking();

        AtomicInteger soldCount = new AtomicInteger();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            metrics.createdBooking();

                            for (int i = 0; i < soldCount.get(); i++) {
                                metrics.soldTicket();
                            }

                            log.atInfo()
                                    .addKeyValue("event", "user.order.success")
                                    .addKeyValue("reason", "success")
                                    .log("Tickets successfully ordered");
                        } else {
                            metrics.failureBooking();
                        }
                    }
                }
        );

        if (screeningId == null || seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Выберите сеанс и хотя бы одно место");
        }

        if (seatIds.stream().distinct().count() != seatIds.size()) {
            throw new IllegalArgumentException("Одно место выбрано несколько раз");
        }

        User user = users.findByEmail(email)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", "user.order.failure")
                            .addKeyValue("reason", "user_not_found")
                            .log("Account does not exist");

                    return new IllegalArgumentException("Аккаунт не найден");
                });

        Screening screening = screenings.findById(screeningId)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", "user.order.failure")
                            .addKeyValue("reason", "screening_not_exists")
                            .log("Screening does not exist");

                    return new IllegalArgumentException("Сеанс не найден");
                });

        AgeRating rating = screening.getMovie().getAgeRating();
        int age = Period.between(
                user.getBirthDate(),
                screening.getStartTime().toLocalDate())
                .getYears();

        if (age < rating.getMinimumAge()) {
            throw new AgeRestrictionException("Для покупки билета необходимо достичь возраста " + rating.getMinimumAge() + " лет");
        }

        List<Long> orderedIds = seatIds.stream().sorted().toList();
        List<Seat> selectedSeats = seats.lockAllByIdIn(orderedIds);

        if (selectedSeats.size() != orderedIds.size()) {
            log.atWarn()
                    .addKeyValue("event", "user.order.failure")
                    .addKeyValue("reason", "seats_not_exists")
                    .log("One or more seats do not exist");

            throw new IllegalArgumentException("Одно или несколько мест не найдены");
        }

        List<Ticket> created = new ArrayList<>();

        for (Seat seat : selectedSeats) {
            if (!seat.getHall().getId().equals(screening.getHall().getId())) {
                log.atWarn()
                        .addKeyValue("event", "user.order.failure")
                        .addKeyValue("reason", "seat_not_part_of_hall")
                        .log("Seat does not belong to the screening hall");

                throw new IllegalArgumentException("Выбранное место не относится к этому залу");
            }

            if (tickets.existsByScreening_IdAndSeat_Id(screeningId, seat.getId())) {
                log.atWarn()
                        .addKeyValue("event", "user.order.failure")
                        .addKeyValue("reason", "seat_is_taken")
                        .log("Seat is already taken");

                throw new SeatUnavailableException();
            }

            Ticket ticket = new Ticket();
            ticket.setUser(user);
            ticket.setScreening(screening);
            ticket.setSeat(seat);
            ticket.setPrice(screening.getPrice());

            created.add(ticket);
        }

        try {
            List<Ticket> saved = tickets.saveAllAndFlush(created);
            soldCount.set(saved.size());

            return saved;
        } catch (DataIntegrityViolationException exception) {
            log.atError()
                    .addKeyValue("event", "user.order.failure")
                    .addKeyValue("reason", "data_integrity_violation")
                    .log("Ticket purchase failed because of a data integrity violation");

            throw new SeatUnavailableException(exception);
        }
    }
}