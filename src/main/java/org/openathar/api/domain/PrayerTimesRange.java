package org.openathar.api.domain;

import java.time.LocalDate;
import java.util.List;

/** Prayer times for every day of an inclusive date range at one location and method. */
public record PrayerTimesRange(
        LocalDate from,
        LocalDate to,
        String method,
        double latitude,
        double longitude,
        double utcOffsetHours,
        List<PrayerTimes> days) {
}
