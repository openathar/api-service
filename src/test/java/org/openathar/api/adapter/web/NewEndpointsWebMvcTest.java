package org.openathar.api.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.CalculationMethod;
import org.openathar.api.domain.CalculationMethod.Threshold;
import org.openathar.api.domain.NextPrayer;
import org.openathar.api.domain.PrayerEvent;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesRange;
import org.openathar.api.port.in.MethodsUseCase;
import org.openathar.api.port.in.PrayerTimesUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PrayerTimesController.class, MethodsController.class})
class NewEndpointsWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PrayerTimesUseCase prayerTimesUseCase;
    @MockitoBean
    PrayerTimesMapper prayerTimesMapper;
    @MockitoBean
    MethodsUseCase methodsUseCase;
    @MockitoBean
    StringRedisTemplate redis;

    private static NextPrayer sample() {
        return new NextPrayer("MWL", 52.52, 13.405, 2.0, LocalDateTime.of(2026, 9, 14, 17, 30),
            new PrayerEvent("asr", LocalDate.of(2026, 9, 14), "16:30"),
            new PrayerEvent("maghrib", LocalDate.of(2026, 9, 14), "19:25"), 6900);
    }

    @Test
    void methodsListsTheCatalogue() throws Exception {
        when(methodsUseCase.list()).thenReturn(List.of(new CalculationMethod("MWL", "Muslim World League", 18.0,
            new Threshold("angle", 17.0), new Threshold("minutes", 1.0), "STANDARD")));
        mvc.perform(get("/v1/methods"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.methods[0].id").value("MWL"))
            .andExpect(jsonPath("$.methods[0].isha.type").value("angle"))
            .andExpect(jsonPath("$.methods[0].maghrib.value").value(1.0))
            .andExpect(jsonPath("$.asr").value("STANDARD"))
            .andExpect(header().string("Cache-Control", "max-age=86400, public"));
    }

    @Test
    void nextWithoutAtIsNotCached() throws Exception {
        when(prayerTimesUseCase.next(anyDouble(), anyDouble(), any(), anyDouble(), any())).thenReturn(sample());
        mvc.perform(get("/v1/prayer-times/next").param("lat", "52.52").param("lon", "13.405").param("utcOffset", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current.name").value("asr"))
            .andExpect(jsonPath("$.next.time").value("19:25"))
            .andExpect(jsonPath("$.secondsUntilNext").value(6900))
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void nextWithAtIsImmutable() throws Exception {
        when(prayerTimesUseCase.next(anyDouble(), anyDouble(), any(), anyDouble(), eq(Instant.parse("2026-09-14T15:30:00Z"))))
            .thenReturn(sample());
        mvc.perform(get("/v1/prayer-times/next").param("lat", "52.52").param("lon", "13.405").param("utcOffset", "2")
                .param("at", "2026-09-14T15:30:00Z"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void rangeIsImmutableAndRejectsBadInputWithTheStandardErrorShape() throws Exception {
        when(prayerTimesUseCase.range(anyDouble(), anyDouble(), any(), any(), any(), anyDouble()))
            .thenReturn(new PrayerTimesRange(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14), "MWL", 52.52, 13.405, 2.0,
                List.<PrayerTimes>of()));
        mvc.perform(get("/v1/prayer-times/range").param("lat", "52.52").param("lon", "13.405")
                .param("from", "2026-09-14").param("to", "2026-09-14").param("utcOffset", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.method").value("MWL"))
            .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
        mvc.perform(get("/v1/prayer-times/range").param("lat", "52.52").param("lon", "13.405").param("from", "2026-09-14"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("missing required parameter: to"));
    }
}
