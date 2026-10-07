package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.DTO.Cinema;
import com.cinema.ticket_booking.DTO.Movie;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.HallRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional
public class CatalogController {
    @Value("${items-on-pages.count:4}")
    private int itemsPerPage;

    private final MovieRepository movies;
    private final CinemaRepository cinemas;
    private final HallRepository halls;
    private final ScreeningRepository screenings;
    private final SeatRepository seats;
    private final TicketRepository tickets;

    @GetMapping("/movies")
    public PageResponse<Movie> movies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "true") boolean available,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDateTime from = date == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : date.atStartOfDay();
        LocalDateTime to = date == null ? LocalDateTime.of(3000, 1, 1, 0, 0) : date.plusDays(1).atStartOfDay();
        int pageSize = size == null ? itemsPerPage : size;
        return PageResponse.from(movies.findCatalogPage(query.trim(), available, cinemaId == null ? 0L : cinemaId, from, to,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(pageSize, 1), 24))));
    }
    @GetMapping("/cinemas")
    public List<Cinema> cinemas() {
        return cinemas.findAll();
    }

    @GetMapping("/movies/{id}")
    public ResponseEntity<Movie> movie(@PathVariable Long id) {
        return movies.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/halls")
    public List<Map<String, Object>> halls() {
        return halls.findAll().stream()
                .map(hall -> Map.<String, Object>of(
                        "id", hall.getId(),
                        "name", hall.getName(),
                        "cinemaId", hall.getCinema().getId()))
                .toList();
    }

    @GetMapping("/screenings")
    public PageResponse<Map<String, Object>> screenings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48));
        Page<Screening> result;

        if (movieId != null) {
            result = screenings.findByMovie_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                    movieId, from, to, pageable);
        } else if (cinemaId != null) {
            result = screenings.findByHall_Cinema_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                    cinemaId, from, to, pageable);
        } else {
            result = screenings.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                    from, to, pageable);
        }

        return PageResponse.from(result.map(this::screeningView));
    }

    @GetMapping("/screenings/{id}")
    public ResponseEntity<Map<String, Object>> screening(@PathVariable Long id) {
        return screenings.findById(id)
                .map(screening -> ResponseEntity.ok(screeningView(screening)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/screenings/{id}/seats")
    public List<Map<String, Object>> seats(@PathVariable Long id) {
        Screening screening = screenings.findById(id).orElseThrow();
        Set<Long> bookedSeatIds = tickets.findByScreening_Id(id).stream()
                .map(ticket -> ticket.getSeat().getId())
                .collect(Collectors.toSet());
        return seats.findByHall_IdOrderByRowNumberAscSeatNumberAsc(screening.getHall().getId()).stream()
                .map(seat -> Map.<String, Object>of(
                        "id", seat.getId(),
                        "row", seat.getRowNumber(),
                        "number", seat.getSeatNumber(),
                        "available", !bookedSeatIds.contains(seat.getId())))
                .toList();
    }

    @PostMapping("/movies")
    public Movie createMovie(@RequestBody Movie movie) {
        return movies.save(movie);
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id, @RequestBody Movie input) {
        return movies.findById(id)
                .map(movie -> {
                    movie.setTitle(input.getTitle());
                    movie.setDescription(input.getDescription());
                    movie.setDurationMinutes(input.getDurationMinutes());
                    movie.setAgeRating(input.getAgeRating());
                    movie.setPosterUrl(input.getPosterUrl());
                    movie.setReleaseDate(input.getReleaseDate());
                    return ResponseEntity.ok(movies.save(movie));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        if (!movies.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        movies.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> screeningView(Screening screening) {
        return Map.of(
                "id", screening.getId(),
                "startTime", screening.getStartTime(),
                "price", screening.getPrice(),
                "movieId", screening.getMovie().getId(),
                "movieTitle", screening.getMovie().getTitle(),
                "posterUrl", screening.getMovie().getPosterUrl(),
                "hallId", screening.getHall().getId(),
                "hallName", screening.getHall().getName(),
                "cinemaId", screening.getHall().getCinema().getId(),
                "cinemaName", screening.getHall().getCinema().getName());
    }

    public record PageResponse<T>(
            List<T> content,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast());
        }
    }
}
