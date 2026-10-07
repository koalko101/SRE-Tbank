package com.cinema.ticket_booking.controller;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {
    @Mock
    private TicketRepository tickets;

    @Mock
    private ScreeningRepository screenings;

    @Mock
    private SeatRepository seats;

    @Mock
    private UserRepository users;

    @InjectMocks
    private TicketController controller;

    @Test
    void ticketListIsScopedToAuthenticatedAccount() {
        Principal principal = () -> "alice@example.com";
        when(tickets.findByUser_EmailOrderByCreatedAtDesc(principal.getName()))
                .thenReturn(List.of());

        assertThat(controller.all(principal)).isEmpty();

        verify(tickets).findByUser_EmailOrderByCreatedAtDesc("alice@example.com");
        verifyNoMoreInteractions(tickets);
    }

    @Test
    void purchaseIsRejectedWhenUserIsUnderMovieAgeRating() {
        Principal principal = () -> "alice@example.com";
        User user = userBornOn(LocalDate.of(2010, 10, 3));
        Screening screening = screening(AgeRating.R16);
        when(users.findByEmail(principal.getName())).thenReturn(Optional.of(user));
        when(screenings.findById(10L)).thenReturn(Optional.of(screening));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.create(
                        new TicketController.TicketRequest(10L, List.of(20L)), principal))
                .isInstanceOf(AgeRestrictionException.class)
                .hasMessageContaining("16 лет");

        verify(tickets, never()).saveAll(anyList());
        verifyNoMoreInteractions(seats);
    }

    @Test
    void purchaseIsAllowedWhenUserMeetsMovieAgeRating() {
        Principal principal = () -> "alice@example.com";
        User user = userBornOn(LocalDate.of(2010, 10, 2));
        Screening screening = screening(AgeRating.R16);
        Seat seat = seatFor(screening.getHall());
        when(users.findByEmail(principal.getName())).thenReturn(Optional.of(user));
        when(screenings.findById(10L)).thenReturn(Optional.of(screening));
        when(seats.findByIdIn(List.of(20L))).thenReturn(List.of(seat));
        when(tickets.existsByScreening_IdAndSeat_Id(10L, 20L)).thenReturn(false);
        when(tickets.saveAll(anyList())).thenAnswer(invocation -> {
            List<Ticket> savedTickets = invocation.getArgument(0);
            savedTickets.forEach(ticket -> ticket.setCreatedAt(LocalDateTime.now()));
            return savedTickets;
        });

        assertThat(controller.create(new TicketController.TicketRequest(10L, List.of(20L)), principal))
                .hasSize(1);

        verify(tickets).saveAll(anyList());
    }

    @Test
    void ageRestrictionUsesForbiddenHttpStatus() {
        assertThat(new ApiExceptionHandler()
                .ageRestriction(new AgeRestrictionException("Возрастное ограничение")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private User userBornOn(LocalDate birthDate) {
        User user = new User();
        user.setBirthDate(birthDate);
        return user;
    }

    private Screening screening(AgeRating rating) {
        Movie movie = new Movie();
        movie.setAgeRating(rating);
        movie.setTitle("Тестовый фильм");

        Cinema cinema = new Cinema();
        cinema.setName("Тестовый кинотеатр");
        Hall hall = new Hall();
        hall.setId(30L);
        hall.setName("Зал 1");
        hall.setCinema(cinema);

        Screening screening = new Screening();
        screening.setId(10L);
        screening.setMovie(movie);
        screening.setHall(hall);
        screening.setPrice(BigDecimal.TEN);
        screening.setStartTime(LocalDateTime.of(2026, 10, 2, 18, 0));
        return screening;
    }

    private Seat seatFor(Hall hall) {
        Seat seat = new Seat();
        seat.setId(20L);
        seat.setHall(hall);
        seat.setRowNumber(1);
        seat.setSeatNumber(1);
        return seat;
    }
}
