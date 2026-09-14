package org.openathar.api.port.in;

import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;

/** Use case: calculate prayer times for a location/date/method. */
public interface PrayerTimesUseCase {

    PrayerTimes calculate(PrayerTimesQuery query);
}