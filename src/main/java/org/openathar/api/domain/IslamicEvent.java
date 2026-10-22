package org.openathar.api.domain;

import java.time.LocalDate;

/** An observance on the Hijri calendar and the Gregorian date it falls on. */
public record IslamicEvent(
        String key,
        String name,
        String note,
        int hijriDay,
        int hijriMonth,
        int hijriYear,
        LocalDate gregorianDate,
        long daysUntil) {
}
