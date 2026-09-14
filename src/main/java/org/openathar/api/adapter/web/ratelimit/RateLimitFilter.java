package org.openathar.api.adapter.web.ratelimit;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Redis-Fixed-Window-Rate-Limit pro Client-IP. Wird Redis nicht erreicht,
 * laeuft der Request ungehindert durch (fail open) — die API darf nicht
 * ausfallen, nur weil der Redis kurz weg ist.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final long limit;
    private final long windowSeconds;

    public RateLimitFilter(
            StringRedisTemplate redis,
            @Value("${athar.ratelimit.enabled:true}") boolean enabled,
            @Value("${athar.ratelimit.limit:60}") long limit,
            @Value("${athar.ratelimit.window-seconds:60}") long windowSeconds) {
        this.redis = redis;
        this.enabled = enabled;
        this.limit = limit;
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (enabled && request.getRequestURI().startsWith("/v1/")) {
            try {
                long windowMillis = windowSeconds * 1000L;
                String key = "athar:ratelimit:" + request.getRemoteAddr() + ":" + (System.currentTimeMillis() / windowMillis);
                Long count = redis.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redis.expire(key, Duration.ofSeconds(windowSeconds));
                }
                if (count != null && count > limit) {
                    response.setStatus(429);
                    response.setHeader("Retry-After", String.valueOf(windowSeconds));
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"rate limit exceeded\"}");
                    return;
                }
            } catch (RuntimeException e) {
                // Redis nicht erreichbar → nicht blockieren.
            }
        }
        chain.doFilter(request, response);
    }
}