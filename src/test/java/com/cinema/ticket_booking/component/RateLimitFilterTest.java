package com.cinema.ticket_booking.component;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RateLimitFilter filter = new RateLimitFilter(redis);

    @Test
    void returns429AndRetryAfterWhenAuthenticationLimitIsReached() throws Exception {
        doReturn(9L * 100_000 + 37)
                .when(redis)
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("37");
        assertThat(response.getContentAsString()).contains("error");
        verify(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void forwardsRequestBelowConfiguredLimit() throws Exception {
        doReturn(8L * 100_000 + 37)
                .when(redis)
                .execute(any(RedisScript.class), anyList(), any(Object[].class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }
}
