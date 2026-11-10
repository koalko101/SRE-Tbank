package com.cinema.ticket_booking.component;

import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

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
    private final MeterRegistry registry;

    
    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
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
        long startNanos = System.nanoTime();
        T result = operation.get();
        bookingDurationSeconds.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        return result;
    }

    public void recordApiRequest(String operation, int status) {
        boolean good = status < 500 && status != 429;
        Counter.builder("cinema.sli.requests")
                .tag("operation", operation)
                .tag("result", good ? "good" : "bad")
                .register(registry)
                .increment();

    }


}
