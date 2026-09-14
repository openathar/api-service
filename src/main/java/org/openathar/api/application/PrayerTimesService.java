package org.openathar.api.application;

import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.port.in.PrayerTimesUseCase;
import org.openathar.core.Method;
import org.openathar.core.PrayerTimesResult;
import org.springframework.stereotype.Service;

/**
 * Calculates prayer times by delegating to athan-core-java — the single
 * source of truth for calculation logic. Thin, stateless wrapper.
 */
@Service
public class PrayerTimesService implements PrayerTimesUseCase {

    @Override
    public PrayerTimes calculate(PrayerTimesQuery query) {
        Method method = Method.valueOf(query.method().toUpperCase());
        PrayerTimesResult result = new org.openathar.core.PrayerTimes(method)
            .getTimes(query.date().getYear(), query.date().getMonthValue(), query.date().getDayOfMonth(),
                query.latitude(), query.longitude());
        return new PrayerTimes(
            query.date(),
            method.name(),
            query.latitude(),
            query.longitude(),
            query.utcOffsetHours(),
            format(result.fajr(), query.utcOffsetHours()),
            format(result.sunrise(), query.utcOffsetHours()),
            format(result.dhuhr(), query.utcOffsetHours()),
            format(result.asr(), query.utcOffsetHours()),
            format(result.sunset(), query.utcOffsetHours()),
            format(result.maghrib(), query.utcOffsetHours()),
            format(result.isha(), query.utcOffsetHours()),
            format(result.midnight(), query.utcOffsetHours()),
            format(result.duhaStart(), query.utcOffsetHours()),
            format(result.duhaEnd(), query.utcOffsetHours()),
            format(result.duhaBest(), query.utcOffsetHours()));
    }

    private String format(long utcMillis, double utcOffsetHours) {
        return org.openathar.core.PrayerTimes.formatLocalTime(utcMillis, utcOffsetHours);
    }
}