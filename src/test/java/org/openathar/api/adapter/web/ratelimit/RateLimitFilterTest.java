package org.openathar.api.adapter.web.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
        return new RateLimitFilter(redis, enabled, limit, limit * 10, windowSeconds, "CF-Connecting-IP");
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
        assertEquals("60", response.getHeader("X-RateLimit-Limit"));
        assertEquals("59", response.getHeader("X-RateLimit-Remaining"));
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
        assertEquals("60", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
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

    @Test
    void unknownApiKeyFallsBackToAnonymousLimit() throws Exception {
        when(redis.hasKey(anyString())).thenReturn(false);
        when(ops.increment(anyString())).thenReturn(1L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/prayer-times");
        request.addHeader("X-API-Key", "ath_does-not-exist");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(true, 60, 60).doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(ops, never()).increment("athar:apikey:usage:ath_does-not-exist");
    }

    @Test
    void knownApiKeyUsesKeyedLimitAndBumpsUsage() throws Exception {
        when(redis.hasKey("athar:apikey:ath_valid")).thenReturn(true);
        when(ops.increment(anyString())).thenReturn(500L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/prayer-times");
        request.addHeader("X-API-Key", "ath_valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // limit=60 -> keyedLimit=600, so 500 stays under the keyed limit even though it's over the anonymous one.
        filter(true, 60, 60).doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("600", response.getHeader("X-RateLimit-Limit"));
        assertEquals("100", response.getHeader("X-RateLimit-Remaining"));
        verify(ops).increment("athar:apikey:usage:ath_valid");
    }

    // ---- Client identity behind the Cloudflare tunnel -------------------------------------
    // All traffic reaches the API from the proxy's LAN address, so getRemoteAddr() is the same
    // for every visitor and they would all share one bucket. Cloudflare sends the real client
    // address in CF-Connecting-IP.

    private List<String> bucketsFor(String remoteAddr, String... headerValues) throws Exception {
        when(ops.increment(anyString())).thenReturn(1L);
        RateLimitFilter f = filter(true, 60, 60);
        for (String header : headerValues) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/qibla");
            request.setRemoteAddr(remoteAddr);
            if (header != null) {
                request.addHeader("CF-Connecting-IP", header);
            }
            f.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(ops, org.mockito.Mockito.atLeastOnce()).increment(keys.capture());
        return keys.getAllValues();
    }

    @Test
    void usesTheCloudflareClientAddressNotTheProxyAddress() throws Exception {
        List<String> keys = bucketsFor("192.168.1.44", "203.0.113.7");
        assertTrue(keys.get(0).contains(":ip:203.0.113.7:"), keys.get(0));
        assertTrue(keys.stream().noneMatch(k -> k.contains("192.168.1.44")), keys.toString());
    }

    @Test
    void differentVisitorsGetSeparateBuckets() throws Exception {
        List<String> keys = bucketsFor("192.168.1.44", "203.0.113.7", "198.51.100.9");
        assertNotEquals(keys.get(0), keys.get(1));
    }

    @Test
    void ipv6AddressIsNormalised() throws Exception {
        List<String> keys = bucketsFor("192.168.1.44", "2001:0db8:0000:0000:0000:0000:0000:0001");
        assertTrue(keys.get(0).contains(":ip:2001:db8:0:0:0:0:0:1:"), keys.get(0));
    }

    @Test
    void fallsBackToTheConnectionAddressWithoutTheHeader() throws Exception {
        List<String> keys = bucketsFor("192.168.1.44", (String) null);
        assertTrue(keys.get(0).contains(":ip:192.168.1.44:"), keys.get(0));
    }

    @Test
    void garbageInTheHeaderIsIgnoredAndCannotPolluteKeys() throws Exception {
        for (String bad : new String[] {"not-an-ip; DROP", "1.2.3.4, 5.6.7.8", "999.1.1.1", "a:b", "x".repeat(300)}) {
            List<String> keys = bucketsFor("192.168.1.44", bad);
            assertTrue(keys.get(keys.size() - 1).contains(":ip:192.168.1.44:"), bad + " -> " + keys);
            org.mockito.Mockito.clearInvocations(ops);
        }
    }
}
