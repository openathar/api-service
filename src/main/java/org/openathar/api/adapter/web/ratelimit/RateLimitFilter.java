package org.openathar.api.adapter.web.ratelimit;

import java.io.IOException;
import java.net.InetAddress;
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
 * Redis fixed-window rate limit, keyed by client IP (see {@link #clientIp}) for anonymous requests
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
    private final String clientIpHeader;

    public RateLimitFilter(
            StringRedisTemplate redis,
            @Value("${athar.ratelimit.enabled:true}") boolean enabled,
            @Value("${athar.ratelimit.limit:60}") long anonymousLimit,
            @Value("${athar.ratelimit.keyed-limit:600}") long keyedLimit,
            @Value("${athar.ratelimit.window-seconds:60}") long windowSeconds,
            @Value("${athar.ratelimit.client-ip-header:CF-Connecting-IP}") String clientIpHeader) {
        this.clientIpHeader = clientIpHeader == null ? "" : clientIpHeader.trim();
        this.redis = redis;
        this.enabled = enabled;
        this.anonymousLimit = anonymousLimit;
        this.keyedLimit = keyedLimit;
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    /**
     * The visitor's address. Behind the Cloudflare tunnel every request reaches this service from
     * the same proxy address, so {@code getRemoteAddr()} alone would put all anonymous visitors
     * into ONE bucket. Cloudflare sends the real address in {@code CF-Connecting-IP}; Traefik
     * replaces {@code X-Forwarded-For}, so that one is no use here.
     *
     * <p>Trust: the header is only believable because the service is reachable solely through
     * the tunnel (no public port). If that ever changes, set
     * {@code athar.ratelimit.client-ip-header} to an empty value.
     *
     * <p>The value ends up in a Redis key, so it must be a single, valid IP literal — anything
     * else (a list, text, an oversized string) is ignored and the connection address is used.
     */
    private String clientIp(HttpServletRequest request) {
        if (!clientIpHeader.isEmpty()) {
            String value = request.getHeader(clientIpHeader);
            if (value != null) {
                String candidate = value.trim();
                if (candidate.length() <= 45 && candidate.matches("[0-9a-fA-F:.]+")) {
                    try {
                        // ofLiteral parses a literal only — it never does a DNS lookup.
                        return InetAddress.ofLiteral(candidate).getHostAddress();
                    } catch (IllegalArgumentException ignored) {
                        // not an IP literal -> fall through
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (enabled && request.getRequestURI().startsWith("/v1/")) {
            try {
                String apiKey = request.getHeader(API_KEY_HEADER);
                boolean keyed = apiKey != null && !apiKey.isBlank()
                    && Boolean.TRUE.equals(redis.hasKey(ApiKeyKeys.hash(apiKey)));
                String identity = keyed ? "key:" + apiKey : "ip:" + clientIp(request);
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
                if (count != null) {
                    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                    response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
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
