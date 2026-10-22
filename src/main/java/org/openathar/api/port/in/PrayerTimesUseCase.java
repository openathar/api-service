package org.openathar.api.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.openathar.api.domain.BatchLocation;
import org.openathar.api.domain.NextPrayer;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.domain.PrayerTimesRange;

/** Use cases around prayer times for a location/date/method. */
public interface PrayerTimesUseCase {

    PrayerTimes calculate(PrayerTimesQuery query);

    /** Every day from {@code from} to {@code to}, inclusive. */
    PrayerTimesRange range(double lat, double lon, LocalDate from, LocalDate to, String method, double utcOffsetHours);

    /** The same date and method for many locations; the result list keeps the input order. */
    List<PrayerTimes> batch(List<BatchLocation> locations, LocalDate date, String method);

    /** Current and next prayer at the given instant, in the location's local time. */
    NextPrayer next(double lat, double lon, String method, double utcOffsetHours, Instant at);
}
