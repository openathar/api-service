package org.openathar.api.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.openathar.api.port.in.ApiKeyUseCase;
import org.openathar.api.port.in.QiblaUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Die Doku verspricht eine Fehlerform fuer alles: {"error": "..."}.
 * Ohne eigene Handler lieferte Spring fuer 404/405/415/kaputtes JSON sein
 * Standardformat {timestamp,status,error,path} — dieser Test haelt das fest.
 */
@WebMvcTest({ApiKeyController.class, QiblaController.class})
class ErrorShapeWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ApiKeyUseCase apiKeyUseCase;
    @MockitoBean
    ApiKeyMapper apiKeyMapper;
    @MockitoBean
    QiblaUseCase qiblaUseCase;
    @MockitoBean
    QiblaMapper qiblaMapper;
    @MockitoBean
    StringRedisTemplate redis;

    @Test
    void unknownPathUsesErrorShape() throws Exception {
        mvc.perform(get("/v1/nope"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("not found: /v1/nope"))
            .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void wrongMethodUsesErrorShape() throws Exception {
        mvc.perform(delete("/v1/qibla").param("lat", "1").param("lon", "1"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.error").value("method DELETE not allowed"));
    }

    @Test
    void wrongContentTypeUsesErrorShape() throws Exception {
        mvc.perform(post("/v1/api-keys").contentType(MediaType.TEXT_PLAIN).content("hello"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error").value("unsupported content type: text/plain (use application/json)"));
    }

    @Test
    void malformedJsonUsesErrorShape() throws Exception {
        mvc.perform(post("/v1/api-keys").contentType(MediaType.APPLICATION_JSON).content("{broken"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("malformed JSON body"));
    }

    @Test
    void unexpectedExceptionUsesErrorShapeWithoutLeakingDetails() throws Exception {
        org.mockito.Mockito.when(qiblaUseCase.calculate(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble()))
            .thenThrow(new IllegalStateException("secret internal detail"));
        mvc.perform(get("/v1/qibla").param("lat", "1").param("lon", "1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("internal error"));
    }
}
