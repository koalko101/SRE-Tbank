package com.cinema.ticket_booking.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final DefaultRedisScript<Long> WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; "
                    + "local ttl = redis.call('TTL', KEYS[1]); "
                    + "return count * 100000 + ttl",
            Long.class);
    private final StringRedisTemplate redis;

    @Value("${app.rate-limit.general-limit:90}")
    private int generalLimit = 90;

    @Value("${app.rate-limit.auth-limit:8}")
    private int authLimit = 8;

    @Value("${app.rate-limit.purchase-limit:6}")
    private int purchaseLimit = 6;

    @Value("${app.rate-limit.admin-limit:120}")
    private int adminLimit = 120;

    @Value("${app.rate-limit.window-seconds:60}")
    private int windowSeconds = 60;

    public RateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod()) || request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = generalLimit;
        String group = "api";
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            limit = authLimit;
            group = "auth";
        } else if ("POST".equals(request.getMethod()) && path.equals("/api/tickets")) {
            limit = purchaseLimit;
            group = "purchase";
        } else if (path.startsWith("/api/admin/")) {
            limit = adminLimit;
            group = "admin";
        }

        String key = "cinema:rate:" + group + ":" + request.getRemoteAddr();
        try {
            Long result = redis.execute(WINDOW_SCRIPT, List.of(key), Integer.toString(windowSeconds));
            if (result == null) {
                serviceUnavailable(response);
                return;
            }
            long currentCount = result / 100000;
            long secondsRemaining = result % 100000;
            if (currentCount > limit) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(Math.max(secondsRemaining, 1)));
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Слишком много запросов. Повторите позже.\"}");
                return;
            }
        } catch (DataAccessException exception) {
            serviceUnavailable(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void serviceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(503);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Сервис временно недоступен. Повторите запрос позже.\"}");
    }
}
