package org.openathar.api.adapter.web.dto;

import java.time.LocalDate;

/** REST response for GET /v1/prayer-times. */
public record PrayerTimesResponse(
        LocalDate date,
        String method,
        double latitude,
        double longitude,
        double utcOffsetHours,
        String fajr,
        String sunrise,
        String dhuhr,
        String asr,
        String sunset,
        String maghrib,
        String isha,
        String midnight,
        String duhaStart,
        String duhaEnd,
        String duhaBest) {
}