package org.openathar.api.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openathar.api.adapter.web.dto.ApiKeyResponse;
import org.openathar.api.adapter.web.dto.ApiKeyUsageResponse;
import org.openathar.api.domain.ApiKey;
import org.openathar.api.domain.ApiKeyUsage;
import org.openathar.api.port.in.ApiKeyUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiKeyController.class)
class ApiKeyControllerWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ApiKeyUseCase useCase;
    @MockitoBean
    ApiKeyMapper mapper;
    @MockitoBean
    StringRedisTemplate redis;

    @Test
    void issuingAKeyReturnsItOnce() throws Exception {
        Instant now = Instant.parse("2026-09-14T12:00:00Z");
        when(useCase.issue(eq("my-widget"))).thenReturn(new ApiKey("ath_abc123", "my-widget", now));
        when(mapper.toResponse(any())).thenReturn(new ApiKeyResponse("ath_abc123", "my-widget", now));

        mvc.perform(post("/v1/api-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"my-widget\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiKey").value("ath_abc123"))
            .andExpect(jsonPath("$.label").value("my-widget"));
    }

    @Test
    void usageForKnownKeyIsReturned() throws Exception {
        Instant now = Instant.parse("2026-09-14T12:00:00Z");
        when(useCase.usage(eq("ath_abc123"))).thenReturn(Optional.of(new ApiKeyUsage("ath_abc123", "my-widget", now, 42L)));
        when(mapper.toUsageResponse(any())).thenReturn(new ApiKeyUsageResponse("my-widget", now, 42L));

        mvc.perform(get("/v1/api-keys/ath_abc123/usage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestCount").value(42));
    }

    @Test
    void usageForUnknownKeyIs404() throws Exception {
        when(useCase.usage(eq("ath_nope"))).thenReturn(Optional.empty());

        mvc.perform(get("/v1/api-keys/ath_nope/usage"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("unknown API key"));
    }
}
