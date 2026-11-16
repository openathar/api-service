package org.openathar.api.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.openathar.api.adapter.web.dto.QiblaResponse;
import org.openathar.api.domain.Qibla;
import org.openathar.api.port.in.QiblaUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A browser only lets JavaScript read response headers that CORS exposes.
 * The quota headers are useless to web clients (e.g. the developer portal)
 * unless they are listed in Access-Control-Expose-Headers.
 */
@WebMvcTest(QiblaController.class)
class CorsExposedHeadersWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    QiblaUseCase qiblaUseCase;
    @MockitoBean
    QiblaMapper qiblaMapper;
    @MockitoBean
    StringRedisTemplate redis;

    @Test
    void quotaHeadersAreExposedToAllowedOrigin() throws Exception {
        when(qiblaUseCase.calculate(anyDouble(), anyDouble())).thenReturn(new Qibla(52.52, 13.405, 136.0));
        when(qiblaMapper.toResponse(any())).thenReturn(new QiblaResponse(52.52, 13.405, 136.0));

        mvc.perform(get("/v1/qibla").param("lat", "52.52").param("lon", "13.405")
                .header("Origin", "https://openathar.org"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://openathar.org"))
            .andExpect(header().string("Access-Control-Expose-Headers",
                "X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After"));
    }
}
