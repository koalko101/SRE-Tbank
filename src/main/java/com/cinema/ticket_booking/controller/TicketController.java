package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.DTO.Ticket;
import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.service.TicketPurchaseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Transactional
public class TicketController {
    private static final Logger log = LoggerFactory.getLogger(TicketController.class);
    private final TicketRepository tickets;
    private final TicketPurchaseService purchaseService;

    @GetMapping
    public List<Map<String, Object>> all(Principal principal) {
        return tickets.findByUser_EmailOrderByCreatedAtDesc(principal.getName()).stream()
                .map(this::view).toList();
    }

    @PostMapping
    public List<Map<String, Object>> create(@RequestBody TicketRequest request, Principal principal) {
        List<Ticket> saved = purchaseService.purchase(request.screeningId(), request.seatIds(), principal.getName());
        log.info("Ticket purchase completed: ticketCount={}", saved.size());
        return saved.stream().map(this::view).toList();
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
        return Map.of("id", ticket.getId(), "price", ticket.getPrice(), "createdAt", ticket.getCreatedAt(),
                "movieTitle", ticket.getScreening().getMovie().getTitle(),
                "startTime", ticket.getScreening().getStartTime(),
                "cinemaName", ticket.getScreening().getHall().getCinema().getName(),
                "hallName", ticket.getScreening().getHall().getName(),
                "row", ticket.getSeat().getRowNumber(), "number", ticket.getSeat().getSeatNumber());
    }

    public record TicketRequest(Long screeningId, List<Long> seatIds) {}
}
