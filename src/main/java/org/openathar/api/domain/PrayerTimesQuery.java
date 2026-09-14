package org.openathar.api.domain;

import java.time.LocalDate;

/**
 * Request for prayer-time calculation: location, date, calculation method
 * and the location's UTC offset in hours (used to render local wall-clock
 * times; V1 uses a numeric offset instead of an IANA timezone — no
 * timezone database in athan-core yet).
 */
public record PrayerTimesQuery(
        double latitude,
        double longitude,
        LocalDate date,
        String method,
        double utcOffsetHours) {

    public PrayerTimesQuery {
        if (latitude < -90 || latitude > 90) throw new IllegalArgumentException("latitude out of range: " + latitude);
        if (longitude < -180 || longitude > 180) throw new IllegalArgumentException("longitude out of range: " + longitude);
        if (method == null || method.isBlank()) throw new IllegalArgumentException("method must not be blank");
        if (utcOffsetHours < -12 || utcOffsetHours > 14) throw new IllegalArgumentException("utcOffsetHours out of range: " + utcOffsetHours);
    }
}