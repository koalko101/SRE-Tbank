package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.repository.HallRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEmptyDataControllerTest {
    @Mock
    private ScreeningRepository screenings;

    @Mock
    private MovieRepository movies;

    @Mock
    private HallRepository halls;

    @Mock
    private TicketRepository tickets;

    @Test
    void emptyScreeningsReturnAnEmptyPage() {
        when(screenings.findAllByOrderByStartTime(any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        Map<String, Object> response = new AdminCatalogController(
                screenings,
                movies,
                halls,
                tickets
        ).allScreenings(0, 20);

        assertThat(response.get("content")).isEqualTo(List.of());
        assertThat(response.get("totalPages")).isEqualTo(0);
        assertThat(response.get("totalElements")).isEqualTo(0L);
    }

    @Test
    void emptyTicketSalesReturnAnEmptyPage() {
        when(tickets.findAll(any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        Map<String, Object> response = new AdminTicketController(tickets).all(0, 20);

        assertThat(response.get("content")).isEqualTo(List.of());
        assertThat(response.get("totalPages")).isEqualTo(0);
        assertThat(response.get("totalElements")).isEqualTo(0L);
    }

    @Test
    void emptyOptionsReturnEmptyMovieAndHallLists() {
        when(movies.findAll(any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 200)));
        when(halls.findAll()).thenReturn(List.of());

        Map<String, Object> response = new AdminCatalogController(
                screenings,
                movies,
                halls,
                tickets
        ).options();

        assertThat(response.get("movies")).isEqualTo(List.of());
        assertThat(response.get("halls")).isEqualTo(List.of());
    }
}
