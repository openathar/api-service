package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class ApiKeyServiceTest {

    /**
     * Keys kann jeder anonym erzeugen — ein unbegrenztes Label waere ein
     * bequemer Weg, Redis mit Datenmuell zu fuellen.
     */
    @Test
    void rejectsOverlongLabelBeforeTouchingRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ApiKeyService service = new ApiKeyService(redis);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> service.issue("x".repeat(65)));
        assertEquals("label too long: max 64 characters", e.getMessage());
        verifyNoInteractions(redis);
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsLabelAtTheLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForHash()).thenReturn(mock(HashOperations.class));
        ApiKeyService service = new ApiKeyService(redis);
        assertEquals("x".repeat(64), service.issue("x".repeat(64)).label());
    }
}
