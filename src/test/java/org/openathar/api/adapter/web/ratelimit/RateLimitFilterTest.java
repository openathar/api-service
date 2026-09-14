package org.openathar.api.adapter.web.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    private RateLimitFilter filter(boolean enabled, long limit, long windowSeconds) {
        when(redis.opsForValue()).thenReturn(ops);
        return new RateLimitFilter(redis, enabled, limit, windowSeconds);
    }

    @Test
    void underLimitPassesThrough() throws Exception {
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/prayer-times");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(true, 60, 60).doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
        verify(redis).expire(anyString(), any(Duration.class));
    }

    @Test
    void overLimitReturns429WithRetryAfter() throws Exception {
        when(ops.increment(anyString())).thenReturn(61L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/prayer-times");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(true, 60, 60).doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertNull(chain.getRequest());
    }

    @Test
    void redisDownFailsOpen() throws Exception {
        when(ops.increment(anyString())).thenThrow(new RuntimeException("redis down"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/prayer-times");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(true, 60, 60).doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void disabledSkipsRedis() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/prayer-times");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(false, 60, 60).doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(ops, never()).increment(anyString());
    }

    @Test
    void nonApiPathSkipsRedis() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(true, 60, 60).doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(ops, never()).increment(anyString());
    }
}