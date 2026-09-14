package org.openathar.api.domain;

import java.time.LocalDate;

/** Prayer times for a location/date, as local wall-clock "HH:mm" strings. */
public record PrayerTimes(
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