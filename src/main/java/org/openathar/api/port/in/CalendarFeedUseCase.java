package org.openathar.api.port.in;

import java.time.LocalDate;

/** Use case: an iCalendar feed of prayer times. */
public interface CalendarFeedUseCase {

    String prayerTimes(double lat, double lon, LocalDate from, int days, String method, double utcOffsetHours,
            String lang, boolean includeSunrise);
}
