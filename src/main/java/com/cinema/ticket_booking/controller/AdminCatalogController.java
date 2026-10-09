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
        return Map.of(
                "movies", movies.findAll(PageRequest.of(0, 200)).getContent(),
                "halls", halls.findAll().stream().map(this::hallView).toList());
    }

    @GetMapping("/screenings")
    public Map<String, Object> allScreenings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = screenings.findAllByOrderByStartTime(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
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
        return screeningView(screenings.save(screening));
    }

    @PutMapping("/screenings/{id}")
    public ResponseEntity<Map<String, Object>> updateScreening(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningInput input) {
        return screenings.findById(id).map(screening -> {
            apply(input, screening);
            return ResponseEntity.ok(screeningView(screening));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/screenings/{id}")
    public ResponseEntity<?> deleteScreening(
            @PathVariable Long id) {
        if (!screenings.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (tickets.existsByScreening_Id(id)) {
            return ResponseEntity.unprocessableContent()
                    .body(Map.of("error", "Нельзя удалить сеанс с проданными билетами"));
        }
        screenings.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void apply(ScreeningInput input, Screening screening) {
        Movie movie = movies.findById(input.movieId())
                .orElseThrow(() -> new IllegalArgumentException("Фильм не найден"));
        Hall hall = halls.findById(input.hallId())
                .orElseThrow(() -> new IllegalArgumentException("Зал не найден"));
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
