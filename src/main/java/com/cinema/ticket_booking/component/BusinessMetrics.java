package com.cinema.ticket_booking.component;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class BusinessMetrics {


    private final Counter registeredSuccess;
    private final Counter registeredFailure;
    private final Counter authSuccess;
    private final Counter authFailure;
    private final Counter bookingCreated;
    private final Counter bookingAttempts;
    private final Counter bookingFailure;
    private final Counter ticketsSold;
    private final Counter movieSearches;

    private final Timer bookingDurationSeconds;

    
    public BusinessMetrics(MeterRegistry registry) {
        registeredSuccess = registry.counter("cinema.registered.success");
        registeredFailure = registry.counter("cinema.registered.failed");
        authSuccess = registry.counter("cinema.auth.success");
        authFailure = registry.counter("cinema.auth.failure");
        bookingCreated = registry.counter("cinema.booking.created");
        bookingAttempts = registry.counter("cinema.booking.attempts");
        bookingFailure = registry.counter("cinema.booking.failure");
        ticketsSold = registry.counter("cinema.tickets.sold");
        movieSearches = registry.counter("cinema.movie.searches");
        bookingDurationSeconds = registry.timer("cinema.booking.duration");
    }

    public void successRegistered() {
        registeredSuccess.increment();
    }

    public void failureRegistered() {
        registeredFailure.increment();
    }

    public void successAuth() {
        authSuccess.increment();
    }

    public void failureAuth() {
        authFailure.increment();
    }

    public void createdBooking() {
        bookingCreated.increment();
    }

    public void attemptBooking() {
        bookingAttempts.increment();
    }

    public void failureBooking() {
        bookingFailure.increment();
    }

    public void soldTicket() {
        ticketsSold.increment();
    }

    public void searchMovie() {
        movieSearches.increment();
    }

    public <T> T durationBooking(Supplier<T> operation) {
        return bookingDurationSeconds.record(operation);
    }


}
