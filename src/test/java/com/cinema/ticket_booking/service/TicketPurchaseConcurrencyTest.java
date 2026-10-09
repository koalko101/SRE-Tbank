package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.DTO.Cinema;
import com.cinema.ticket_booking.DTO.Hall;
import com.cinema.ticket_booking.DTO.Movie;
import com.cinema.ticket_booking.DTO.Role;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.DTO.Seat;
import com.cinema.ticket_booking.DTO.User;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.HallRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TicketPurchaseConcurrencyTest {
    private static final String CUSTOMER_EMAIL = "concurrent@example.com";

    @Autowired
    private TicketPurchaseService purchaseService;

    @Autowired
    private CinemaRepository cinemas;

    @Autowired
    private HallRepository halls;

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private UserRepository users;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void onlyOneConcurrentRequestCanPurchaseTheSameSeat() throws Exception {
        Booking booking = createBooking();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(purchaseAttempt(booking, ready, start));
            var second = executor.submit(purchaseAttempt(booking, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Boolean> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertThat(outcomes).containsExactlyInAnyOrder(true, false);
            assertThat(tickets.findByScreening_Id(booking.screeningId())).hasSize(1);
        }
    }

    private Callable<Boolean> purchaseAttempt(
            Booking booking,
            CountDownLatch ready,
            CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent purchase did not start");
            }

            try {
                purchaseService.purchase(
                        booking.screeningId(),
                        List.of(booking.seatId()),
                        booking.email()
                );
                return true;
            } catch (SeatUnavailableException exception) {
                return false;
            }
        };
    }

    private Booking createBooking() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transaction.execute(status -> {
            Cinema cinema = new Cinema();
            cinema.setName("Concurrency Cinema");
            cinema.setAddress("Test Street, 1");
            cinema = cinemas.saveAndFlush(cinema);

            Hall hall = new Hall();
            hall.setCinema(cinema);
            hall.setName("Main Hall");
            hall = halls.saveAndFlush(hall);

            Movie movie = new Movie();
            movie.setTitle("Concurrency Test");
            movie.setDescription("Test movie");
            movie.setDurationMinutes(100);
            movie.setAgeRating(AgeRating.R0);
            movie.setPosterUrl("https://example.com/poster.jpg");
            movie.setReleaseDate(LocalDate.now());
            movie = movies.saveAndFlush(movie);

            Screening screening = new Screening();
            screening.setHall(hall);
            screening.setMovie(movie);
            screening.setStartTime(LocalDateTime.now().plusDays(2));
            screening.setPrice(BigDecimal.TEN);
            screening = screenings.saveAndFlush(screening);

            Seat seat = new Seat();
            seat.setHall(hall);
            seat.setRowNumber(1);
            seat.setSeatNumber(1);
            seat = seats.saveAndFlush(seat);

            User user = new User();
            user.setName("Concurrent User");
            user.setEmail(CUSTOMER_EMAIL);
            user.setPhone("+375291234567");
            user.setBirthDate(LocalDate.of(1990, 1, 1));
            user.setRole(Role.USER);
            users.saveAndFlush(user);

            return new Booking(screening.getId(), seat.getId(), CUSTOMER_EMAIL);
        });
    }

    private record Booking(Long screeningId, Long seatId, String email) {}
}
