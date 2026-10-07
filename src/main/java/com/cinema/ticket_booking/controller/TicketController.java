package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.DTO.Seat;
import com.cinema.ticket_booking.DTO.Ticket;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.security.Principal;
import java.util.ArrayList;
import java.time.Period;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Transactional
public class TicketController {
    private final TicketRepository tickets;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    private final UserRepository users;

    @GetMapping
    public List<Map<String, Object>> all(Principal principal) {
        return tickets.findByUser_EmailOrderByCreatedAtDesc(principal.getName()).stream()
                .map(this::view)
                .toList();
    }

    @PostMapping
    public List<Map<String, Object>> create(@RequestBody TicketRequest request, Principal principal) {
        if (request.screeningId() == null || request.seatIds() == null || request.seatIds().isEmpty()) {
            throw new IllegalArgumentException("Выберите сеанс и хотя бы одно место");
        }
        if (request.seatIds().stream().distinct().count() != request.seatIds().size()) {
            throw new IllegalArgumentException("Одно место выбрано несколько раз");
        }

        User user = users.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Аккаунт не найден"));
        Screening screening = screenings.findById(request.screeningId())
                .orElseThrow(() -> new IllegalArgumentException("Сеанс не найден"));
        AgeRating ageRating = screening.getMovie().getAgeRating();
        int ageAtScreening = Period.between(user.getBirthDate(), screening.getStartTime().toLocalDate()).getYears();
        if (ageAtScreening < ageRating.getMinimumAge()) {
            throw new AgeRestrictionException(
                    "Для покупки билета на этот фильм необходимо достичь возраста "
                            + ageRating.getMinimumAge() + " лет");
        }

        List<Seat> selectedSeats = seats.findByIdIn(request.seatIds());
        if (selectedSeats.size() != request.seatIds().size()) {
            throw new IllegalArgumentException("Одно или несколько мест не найдены");
        }

        List<Ticket> newTickets = new ArrayList<>();
        for (Seat seat : selectedSeats) {
            if (!seat.getHall().getId().equals(screening.getHall().getId())) {
                throw new IllegalArgumentException("Выбранное место не относится к этому залу");
            }
            if (tickets.existsByScreening_IdAndSeat_Id(screening.getId(), seat.getId())) {
                throw new IllegalArgumentException("Одно из выбранных мест уже занято");
            }

            Ticket ticket = new Ticket();
            ticket.setUser(user);
            ticket.setScreening(screening);
            ticket.setSeat(seat);
            ticket.setPrice(screening.getPrice());
            newTickets.add(ticket);
        }

        return tickets.saveAll(newTickets).stream().map(this::view).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        return tickets.findByIdAndUser_Email(id, principal.getName())
                .map(ticket -> {
                    tickets.delete(ticket);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> view(Ticket ticket) {
        return Map.of(
                "id", ticket.getId(),
                "price", ticket.getPrice(),
                "createdAt", ticket.getCreatedAt(),
                "movieTitle", ticket.getScreening().getMovie().getTitle(),
                "startTime", ticket.getScreening().getStartTime(),
                "cinemaName", ticket.getScreening().getHall().getCinema().getName(),
                "hallName", ticket.getScreening().getHall().getName(),
                "row", ticket.getSeat().getRowNumber(),
                "number", ticket.getSeat().getSeatNumber());
    }

    public record TicketRequest(Long screeningId, List<Long> seatIds) {}
}
