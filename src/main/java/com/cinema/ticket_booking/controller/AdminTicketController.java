package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
@Transactional
public class AdminTicketController {
    private final TicketRepository tickets;

    @GetMapping
    public Map<String, Object> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = tickets.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        var content = result.getContent().stream()
                .map(ticket -> Map.of(
                        "id", ticket.getId(),
                        "customer", ticket.getUser().getName(),
                        "email", ticket.getUser().getEmail(),
                        "movie", ticket.getScreening().getMovie().getTitle(),
                        "cinema", ticket.getScreening().getHall().getCinema().getName(),
                        "hall", ticket.getScreening().getHall().getName(),
                        "startTime", ticket.getScreening().getStartTime(),
                        "row", ticket.getSeat().getRowNumber(),
                        "seat", ticket.getSeat().getSeatNumber(),
                        "price", ticket.getPrice()))
                .toList();
        return Map.of(
                "content", content,
                "number", result.getNumber(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements());
    }
}
