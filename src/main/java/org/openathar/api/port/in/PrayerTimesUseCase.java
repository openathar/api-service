package org.openathar.api.port.in;

import java.time.Instant;
import java.time.LocalDate;

import org.openathar.api.domain.NextPrayer;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.domain.PrayerTimesRange;

/** Use cases around prayer times for a location/date/method. */
public interface PrayerTimesUseCase {

    PrayerTimes calculate(PrayerTimesQuery query);

    /** Every day from {@code from} to {@code to}, inclusive. */
    PrayerTimesRange range(double lat, double lon, LocalDate from, LocalDate to, String method, double utcOffsetHours);

    /** Current and next prayer at the given instant, in the location's local time. */
    NextPrayer next(double lat, double lon, String method, double utcOffsetHours, Instant at);
}
