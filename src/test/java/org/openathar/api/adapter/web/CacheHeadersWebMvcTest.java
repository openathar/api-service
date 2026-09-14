package org.openathar.api.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.openathar.api.adapter.web.dto.HijriResponse;
import org.openathar.api.adapter.web.dto.PrayerTimesResponse;
import org.openathar.api.adapter.web.dto.QiblaResponse;
import org.openathar.api.domain.HijriDate;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.Qibla;
import org.openathar.api.port.in.HijriUseCase;
import org.openathar.api.port.in.PrayerTimesUseCase;
import org.openathar.api.port.in.QiblaUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PrayerTimesController.class, QiblaController.class, HijriController.class})
class CacheHeadersWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PrayerTimesUseCase prayerTimesUseCase;
    @MockitoBean
    PrayerTimesMapper prayerTimesMapper;
    @MockitoBean
    QiblaUseCase qiblaUseCase;
    @MockitoBean
    QiblaMapper qiblaMapper;
    @MockitoBean
    HijriUseCase hijriUseCase;
    @MockitoBean
    HijriMapper hijriMapper;
    @MockitoBean
    StringRedisTemplate redis;

    @Test
    void prayerTimesSuccessIsCachedImmutable() throws Exception {
        when(prayerTimesUseCase.calculate(any())).thenReturn(new PrayerTimes(
            LocalDate.of(2026, 9, 14), "MWL", 52.52, 13.405, 2.0,
            "04:38", "06:39", "13:02", "16:30", "19:24", "19:25", "21:17", "01:02", "06:54", "12:52", "09:51"));
        when(prayerTimesMapper.toResponse(any())).thenReturn(new PrayerTimesResponse(
            LocalDate.of(2026, 9, 14), "MWL", 52.52, 13.405, 2.0,
            "04:38", "06:39", "13:02", "16:30", "19:24", "19:25", "21:17", "01:02", "06:54", "12:52", "09:51"));

        mvc.perform(get("/v1/prayer-times")
                .param("lat", "52.52").param("lon", "13.405")
                .param("date", "2026-09-14").param("method", "MWL").param("utcOffset", "2"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void qiblaSuccessIsCachedImmutable() throws Exception {
        when(qiblaUseCase.calculate(anyDouble(), anyDouble())).thenReturn(new Qibla(52.52, 13.405, 123.4));
        when(qiblaMapper.toResponse(any())).thenReturn(new QiblaResponse(52.52, 13.405, 123.4));

        mvc.perform(get("/v1/qibla").param("lat", "52.52").param("lon", "13.405"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void hijriSuccessIsCachedImmutable() throws Exception {
        when(hijriUseCase.convert(any(), any())).thenReturn(new HijriDate(LocalDate.of(2026, 9, 14), 3, 4, 1448, "Rabi' al-thani"));
        when(hijriMapper.toResponse(any())).thenReturn(new HijriResponse(LocalDate.of(2026, 9, 14), 3, 4, 1448, "Rabi' al-thani"));

        mvc.perform(get("/v1/hijri").param("date", "2026-09-14").param("locale", "en"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void badRequestIsNotCached() throws Exception {
        mvc.perform(get("/v1/qibla").param("lat", "999").param("lon", "13"))
            .andExpect(status().isBadRequest())
            .andExpect(header().doesNotExist("Cache-Control"));
    }
}