package com.cinema.ticket_booking.component;

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SliMetricsFilter extends OncePerRequestFilter {

    private static final Pattern MOVIE_ID = Pattern.compile("/api/movies/\\d+");
    private static final Pattern SCREENING_ID = Pattern.compile("/api/screenings/\\d+");
    private static final Pattern SCREENING_SEATS = Pattern.compile("/api/screenings/\\d+/seats");

    private final BusinessMetrics metrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("cinema-blackbox".equals(request.getHeader("X-Synthetic-Probe"))) {
            filterChain.doFilter(request, response);
            return;
        }
        String operation = operation(request);
        if (operation == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } catch (IOException | ServletException | RuntimeException exception) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            throw exception;
        } finally {
            metrics.recordApiRequest(operation, status);
        }
    }

    private String operation(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && "/api/auth/register".equals(path)) return "registration";
        if ("POST".equals(method) && "/api/auth/login".equals(path)) return "authentication";
        if ("POST".equals(method) && "/api/tickets".equals(path)) return "booking";
        if ("GET".equals(method) && "/api/tickets".equals(path)) return "ticket_history";
        if ("GET".equals(method) && "/api/movies".equals(path)) {
            return request.getParameter("query") == null || request.getParameter("query").isBlank()
                    ? "catalog" : "movie_search";
        }
        if ("GET".equals(method) && MOVIE_ID.matcher(path).matches()) return "movie_details";
        if ("GET".equals(method) && "/api/cinemas".equals(path)) return "cinema_list";
        if ("GET".equals(method) && SCREENING_SEATS.matcher(path).matches()) return "seat_map";
        if ("GET".equals(method) && ("/api/screenings".equals(path)
                || SCREENING_ID.matcher(path).matches())) return "screenings";
        if (path.startsWith("/api/admin/")) return "admin_catalog";
        return null;
    }
}
