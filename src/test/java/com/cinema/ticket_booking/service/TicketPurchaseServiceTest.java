package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.DTO.Cinema;
import com.cinema.ticket_booking.DTO.Hall;
import com.cinema.ticket_booking.DTO.Movie;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.DTO.Seat;
import com.cinema.ticket_booking.DTO.Ticket;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPurchaseServiceTest {
    @Mock TicketRepository tickets;
    @Mock ScreeningRepository screenings;
    @Mock SeatRepository seats;
    @Mock UserRepository users;
    @InjectMocks TicketPurchaseService service;

    private Screening screening;
    private Seat seat;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        Movie movie = new Movie();
        movie.setAgeRating(AgeRating.R16);
        Hall hall = new Hall();
        hall.setId(30L);
        hall.setCinema(new Cinema());
        screening = new Screening();
        screening.setId(10L);
        screening.setMovie(movie);
        screening.setHall(hall);
        screening.setPrice(BigDecimal.TEN);
        screening.setStartTime(LocalDateTime.now().plusDays(1));
        seat = new Seat();
        seat.setId(20L);
        seat.setHall(hall);
    }

    @Test
    void locksSeatsBeforeCheckingAndSavingTickets() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(screenings.findById(10L)).thenReturn(Optional.of(screening));
        when(seats.lockAllByIdIn(List.of(20L))).thenReturn(List.of(seat));
        when(tickets.saveAllAndFlush(anyList())).thenAnswer(call -> call.getArgument(0));

        List<Ticket> result = service.purchase(10L, List.of(20L), "alice@example.com");

        assertThat(result).hasSize(1);
        InOrder order = inOrder(seats, tickets);
        order.verify(seats).lockAllByIdIn(List.of(20L));
        order.verify(tickets).existsByScreening_IdAndSeat_Id(10L, 20L);
        order.verify(tickets).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsAlreadySoldSeatBeforeSavingAnyTicket() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(screenings.findById(10L)).thenReturn(Optional.of(screening));
        when(seats.lockAllByIdIn(List.of(20L))).thenReturn(List.of(seat));
        when(tickets.existsByScreening_IdAndSeat_Id(10L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> service.purchase(10L, List.of(20L), "alice@example.com"))
                .isInstanceOf(SeatUnavailableException.class);
        verify(tickets, never()).saveAllAndFlush(anyList());
    }

    @Test
    void translatesDatabaseSeatConflictIntoUnavailableSeat() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(screenings.findById(10L)).thenReturn(Optional.of(screening));
        when(seats.lockAllByIdIn(List.of(20L))).thenReturn(List.of(seat));
        when(tickets.saveAllAndFlush(anyList()))
                .thenThrow(new DataIntegrityViolationException("duplicate screening and seat"));

        assertThatThrownBy(() -> service.purchase(10L, List.of(20L), "alice@example.com"))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void rejectsDuplicateSeatsInOneRequest() {
        assertThatThrownBy(() -> service.purchase(10L, List.of(20L, 20L), "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(users, never()).findByEmail("alice@example.com");
    }
}
