package org.openathar.api.application;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.openathar.api.domain.ApiKey;
import org.openathar.api.domain.ApiKeyUsage;
import org.openathar.api.port.in.ApiKeyUseCase;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Issues free, self-serve API keys and reports their usage. V1 storage is
 * Redis (already the only stateful dependency this service has) — no TTL,
 * so keys persist until explicitly revoked (not yet supported). Once
 * Postgres lands for user-sync data, this is the first candidate to move
 * over, but Redis is enough for V1 scale and keeps the service dependency
 * count at zero.
 */
@Service
public class ApiKeyService implements ApiKeyUseCase {

    static final int MAX_LABEL_LENGTH = 64;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public ApiKeyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public ApiKey issue(String label) {
        String safeLabel = (label == null || label.isBlank()) ? "unlabeled" : label.trim();
        // Keys are issued anonymously; an unbounded label would be a free way
        // to fill Redis. 64 chars is plenty for "app or project name".
        if (safeLabel.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("label too long: max " + MAX_LABEL_LENGTH + " characters");
        }
        String value = "ath_" + randomToken();
        Instant createdAt = Instant.now();
        redis.opsForHash().putAll(ApiKeyKeys.hash(value), Map.of(
            "label", safeLabel,
            "createdAt", createdAt.toString()));
        return new ApiKey(value, safeLabel, createdAt);
    }

    @Override
    public Optional<ApiKeyUsage> usage(String value) {
        Map<Object, Object> hash = redis.<Object, Object>opsForHash().entries(ApiKeyKeys.hash(value));
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        String label = String.valueOf(hash.get("label"));
        Instant createdAt = Instant.parse(String.valueOf(hash.get("createdAt")));
        String rawCount = redis.opsForValue().get(ApiKeyKeys.usage(value));
        long requestCount = rawCount != null ? Long.parseLong(rawCount) : 0L;
        return Optional.of(new ApiKeyUsage(value, label, createdAt, requestCount));
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
