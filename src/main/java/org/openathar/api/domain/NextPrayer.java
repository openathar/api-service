package org.openathar.api.domain;

import java.time.LocalDateTime;

/** Which prayer is current, which is next, and how long until it. */
public record NextPrayer(
        String method,
        double latitude,
        double longitude,
        double utcOffsetHours,
        LocalDateTime localTime,
        PrayerEvent current,
        PrayerEvent next,
        long secondsUntilNext) {
}
