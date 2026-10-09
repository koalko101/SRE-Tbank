package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.DTO.Movie;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {
    private final MovieRepository movies;
    private final ScreeningRepository screenings;

    @GetMapping
    public Map<String, Object> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = movies.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "releaseDate")));
        return Map.of(
                "content", result.getContent(),
                "number", result.getNumber(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements());
    }

    @PostMapping
    public Movie create(
            @Valid @RequestBody MovieInput input) {
        return movies.save(input.toMovie(new Movie()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> update(
            @PathVariable Long id,
            @Valid @RequestBody MovieInput input) {
        return movies.findById(id).map(movie -> ResponseEntity.ok(movies.save(input.toMovie(movie))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id) {
        if (!movies.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (screenings.existsByMovie_Id(id)) {
            return ResponseEntity.unprocessableContent()
                    .body(Map.of("error", "Нельзя удалить фильм с сеансами"));
        }
        movies.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record MovieInput(
            @NotBlank
            @Size(max = 255)
            String title,
            @NotBlank
            @Size(max = 255)
            String description,
            @Min(1)
            int durationMinutes,
            @NotNull
            AgeRating ageRating,
            @NotBlank
            @URL
            String posterUrl,
            @NotNull
            LocalDate releaseDate) {
        Movie toMovie(Movie movie) {
            movie.setTitle(title.trim());
            movie.setDescription(description.trim());
            movie.setDurationMinutes(durationMinutes);
            movie.setAgeRating(ageRating);
            movie.setPosterUrl(posterUrl);
            movie.setReleaseDate(releaseDate);
            return movie;
        }
    }
}
