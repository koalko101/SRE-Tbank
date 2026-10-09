package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.DTO.Seat;
import com.cinema.ticket_booking.DTO.Ticket;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.controller.AgeRestrictionException;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPurchaseService {
    private final TicketRepository tickets;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    private final UserRepository users;

    @Transactional
    public List<Ticket> purchase(Long screeningId, List<Long> seatIds, String email) {
        if (screeningId == null || seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Выберите сеанс и хотя бы одно место");
        }
        if (seatIds.stream().distinct().count() != seatIds.size()) {
            throw new IllegalArgumentException("Одно место выбрано несколько раз");
        }
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден"));
        Screening screening = screenings.findById(screeningId)
                .orElseThrow(() -> new IllegalArgumentException("Сеанс не найден"));
        AgeRating rating = screening.getMovie().getAgeRating();
        int age = Period.between(user.getBirthDate(), screening.getStartTime().toLocalDate()).getYears();
        if (age < rating.getMinimumAge()) {
            throw new AgeRestrictionException("Для покупки билета необходимо достичь возраста "
                    + rating.getMinimumAge() + " лет");
        }

        List<Long> orderedIds = seatIds.stream().sorted().toList();
        List<Seat> selectedSeats = seats.lockAllByIdIn(orderedIds);
        if (selectedSeats.size() != orderedIds.size()) {
            throw new IllegalArgumentException("Одно или несколько мест не найдены");
        }
        List<Ticket> created = new ArrayList<>();
        for (Seat seat : selectedSeats) {
            if (!seat.getHall().getId().equals(screening.getHall().getId())) {
                throw new IllegalArgumentException("Выбранное место не относится к этому залу");
            }
            if (tickets.existsByScreening_IdAndSeat_Id(screeningId, seat.getId())) {
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
            return tickets.saveAllAndFlush(created);
        } catch (DataIntegrityViolationException exception) {
            throw new SeatUnavailableException(exception);
        }
    }
}
