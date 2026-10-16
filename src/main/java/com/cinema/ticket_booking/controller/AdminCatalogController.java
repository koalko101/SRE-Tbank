package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.DTO.Hall;
import com.cinema.ticket_booking.DTO.Movie;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.repository.HallRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Transactional
public class AdminCatalogController {
    private final ScreeningRepository screenings;
    private final MovieRepository movies;
    private final HallRepository halls;
    private final TicketRepository tickets;

    @GetMapping("/options")
    public Map<String, Object> options() {
        log.atInfo()
                .addKeyValue("event", "admin.panel.view")
                .addKeyValue("reason", "view_catalog_options")
                .log("Admin viewed catalog options");

        return Map.of(
                "movies", movies.findAll(PageRequest.of(0, 200)).getContent(),
                "halls", halls.findAll().stream().map(this::hallView).toList());
    }

    @GetMapping("/screenings")
    public Map<String, Object> allScreenings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 50);

        log.atInfo()
                .addKeyValue("event", "admin.panel.view")
                .addKeyValue("reason", "view_screenings")
                .addKeyValue("page", pageNumber)
                .addKeyValue("pageSize", pageSize)
                .log("Admin viewed screenings");

        var result = screenings.findAllByOrderByStartTime(
                PageRequest.of(pageNumber, pageSize));

        return Map.of(
                "content", result.getContent().stream().map(this::screeningView).toList(),
                "number", result.getNumber(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements());
    }

    @PostMapping("/screenings")
    public Map<String, Object> createScreening(
            @Valid @RequestBody ScreeningInput input) {

        Screening screening = new Screening();
        apply(input, screening);

        Screening saved = screenings.save(screening);

        log.atInfo()
                .addKeyValue("event", "admin.panel.create")
                .addKeyValue("reason", "create_screening")
                .addKeyValue("screeningId", saved.getId())
                .addKeyValue("movieId", input.movieId())
                .addKeyValue("hallId", input.hallId())
                .log("Screening created");

        return screeningView(saved);
    }

    @PutMapping("/screenings/{id}")
    public ResponseEntity<Map<String, Object>> updateScreening(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningInput input) {

        return screenings.findById(id).map(screening -> {
            apply(input, screening);

            Screening updated = screenings.save(screening);

            log.atInfo()
                    .addKeyValue("event", "admin.panel.change")
                    .addKeyValue("reason", "update_screening")
                    .addKeyValue("screeningId", id)
                    .addKeyValue("movieId", input.movieId())
                    .addKeyValue("hallId", input.hallId())
                    .log("Screening updated");

            return ResponseEntity.ok(screeningView(updated));
        }).orElseGet(() -> {
            log.atWarn()
                    .addKeyValue("event", "admin.panel.change.failure")
                    .addKeyValue("reason", "screening_not_found")
                    .addKeyValue("screeningId", id)
                    .log("Screening update rejected");

            return ResponseEntity.<Map<String, Object>>notFound().build();
        });
    }

    @DeleteMapping("/screenings/{id}")
    public ResponseEntity<?> deleteScreening(
            @PathVariable Long id) {

        if (!screenings.existsById(id)) {
            log.atWarn()
                    .addKeyValue("event", "admin.panel.delete.failure")
                    .addKeyValue("reason", "screening_not_found")
                    .addKeyValue("screeningId", id)
                    .log("Screening deletion rejected");

            return ResponseEntity.notFound().build();
        }

        if (tickets.existsByScreening_Id(id)) {
            log.atWarn()
                    .addKeyValue("event", "admin.panel.delete.failure")
                    .addKeyValue("reason", "screening_has_sold_tickets")
                    .addKeyValue("screeningId", id)
                    .log("Screening deletion rejected");

            return ResponseEntity.unprocessableContent()
                    .body(Map.of("error", "Нельзя удалить сеанс с проданными билетами"));
        }

        screenings.deleteById(id);

        log.atInfo()
                .addKeyValue("event", "admin.panel.delete")
                .addKeyValue("reason", "delete_screening")
                .addKeyValue("screeningId", id)
                .log("Screening deleted");

        return ResponseEntity.noContent().build();
    }

    private void apply(ScreeningInput input, Screening screening) {
        Movie movie = movies.findById(input.movieId())
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", "admin.panel.screening.failure")
                            .addKeyValue("reason", "movie_not_found")
                            .addKeyValue("movieId", input.movieId())
                            .log("Screening operation rejected");

                    return new IllegalArgumentException("Фильм не найден");
                });

        Hall hall = halls.findById(input.hallId())
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", "admin.panel.screening.failure")
                            .addKeyValue("reason", "hall_not_found")
                            .addKeyValue("hallId", input.hallId())
                            .log("Screening operation rejected");

                    return new IllegalArgumentException("Зал не найден");
                });

        screening.setMovie(movie);
        screening.setHall(hall);
        screening.setStartTime(input.startTime());
        screening.setPrice(input.price());
    }

    private Map<String, Object> screeningView(Screening screening) {
        return Map.of(
                "id", screening.getId(),
                "movieId", screening.getMovie().getId(),
                "movieTitle", screening.getMovie().getTitle(),
                "hallId", screening.getHall().getId(),
                "hallName", screening.getHall().getName(),
                "cinemaName", screening.getHall().getCinema().getName(),
                "startTime", screening.getStartTime(),
                "price", screening.getPrice());
    }

    private Map<String, Object> hallView(Hall hall) {
        return Map.of(
                "id", hall.getId(),
                "name", hall.getName(),
                "cinemaId", hall.getCinema().getId(),
                "cinemaName", hall.getCinema().getName());
    }

    public record ScreeningInput(
            @NotNull Long movieId,
            @NotNull Long hallId,
            @NotNull @Future LocalDateTime startTime,
            @NotNull @DecimalMin("0.00") BigDecimal price) {}
}
