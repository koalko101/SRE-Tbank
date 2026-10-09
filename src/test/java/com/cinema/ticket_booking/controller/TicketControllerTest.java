package com.cinema.ticket_booking.controller;

import com.cinema.ticket_booking.repository.TicketRepository;
import com.cinema.ticket_booking.service.TicketPurchaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {
    @Mock TicketRepository tickets;
    @Mock TicketPurchaseService purchaseService;
    @InjectMocks TicketController controller;

    @Test
    void ticketListIsScopedToAuthenticatedAccount() {
        Principal principal = () -> "alice@example.com";
        when(tickets.findByUser_EmailOrderByCreatedAtDesc(principal.getName())).thenReturn(List.of());
        assertThat(controller.all(principal)).isEmpty();
        verify(tickets).findByUser_EmailOrderByCreatedAtDesc(principal.getName());
    }

    @Test
    void purchaseUsesAuthenticatedPrincipalAndRequestedSeats() {
        Principal principal = () -> "alice@example.com";
        when(purchaseService.purchase(10L, List.of(20L), principal.getName())).thenReturn(List.of());
        assertThat(controller.create(new TicketController.TicketRequest(10L, List.of(20L)), principal)).isEmpty();
        verify(purchaseService).purchase(10L, List.of(20L), "alice@example.com");
    }

    @Test
    void ageAndSeatConflictResponsesAreMapped() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        assertThat(handler.ageRestriction(new AgeRestrictionException("age")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.seatUnavailable(new com.cinema.ticket_booking.service.SeatUnavailableException())
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
