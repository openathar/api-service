package org.openathar.api.adapter.web.ratelimit;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openathar.api.application.ApiKeyKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Redis fixed-window rate limit, keyed by client IP for anonymous requests
 * or by API key (X-API-Key header, see {@code ApiKeyController}) for a
 * higher quota. An unrecognized key is treated as anonymous, not rejected
 * — a typo in a header should never turn into a 401 for a free API. Fails
 * open when Redis is unreachable: the API never goes down because the
 * limiter can't reach its store.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final long anonymousLimit;
    private final long keyedLimit;
    private final long windowSeconds;

    public RateLimitFilter(
            StringRedisTemplate redis,
            @Value("${athar.ratelimit.enabled:true}") boolean enabled,
            @Value("${athar.ratelimit.limit:60}") long anonymousLimit,
            @Value("${athar.ratelimit.keyed-limit:600}") long keyedLimit,
            @Value("${athar.ratelimit.window-seconds:60}") long windowSeconds) {
        this.redis = redis;
        this.enabled = enabled;
        this.anonymousLimit = anonymousLimit;
        this.keyedLimit = keyedLimit;
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (enabled && request.getRequestURI().startsWith("/v1/")) {
            try {
                String apiKey = request.getHeader(API_KEY_HEADER);
                boolean keyed = apiKey != null && !apiKey.isBlank()
                    && Boolean.TRUE.equals(redis.hasKey(ApiKeyKeys.hash(apiKey)));
                String identity = keyed ? "key:" + apiKey : "ip:" + request.getRemoteAddr();
                long limit = keyed ? keyedLimit : anonymousLimit;

                long windowMillis = windowSeconds * 1000L;
                String bucketKey = "athar:ratelimit:" + identity + ":" + (System.currentTimeMillis() / windowMillis);
                Long count = redis.opsForValue().increment(bucketKey);
                if (count != null && count == 1) {
                    redis.expire(bucketKey, Duration.ofSeconds(windowSeconds));
                }
                if (keyed) {
                    redis.opsForValue().increment(ApiKeyKeys.usage(apiKey));
                }
                if (count != null && count > limit) {
                    response.setStatus(429);
                    response.setHeader("Retry-After", String.valueOf(windowSeconds));
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"rate limit exceeded\"}");
                    return;
                }
            } catch (RuntimeException e) {
                // Redis unreachable -> do not block.
            }
        }
        chain.doFilter(request, response);
    }
}
