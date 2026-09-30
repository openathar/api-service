package org.openathar.api.application;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.function.LongFunction;
import java.util.stream.Collectors;

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

    private static final long DAY_MILLIS = 86_400_000L;

    @Override
    public PrayerTimes calculate(PrayerTimesQuery query) {
        Method method = parseMethod(query.method());
        PrayerTimesResult result = new org.openathar.core.PrayerTimes(method)
            .getTimes(query.date().getYear(), query.date().getMonthValue(), query.date().getDayOfMonth(),
                query.latitude(), query.longitude());
        // Plausibility window for the requested date. Events that do not occur
        // (sunrise during polar day, Isha without twilight and without a night
        // to divide) come out of the core as NaN -> epoch 0, and anything
        // derived from them (Duha, Maghrib = sunset + 1 min) lands just as far
        // off. A real event for date D always falls within D-1 .. D+2 in UTC,
        // whatever the longitude — outside that window it is a sentinel, not a
        // time, and is returned as null rather than as a made-up clock value.
        long dayStart = query.date().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long earliest = dayStart - DAY_MILLIS;
        long latest = dayStart + 2 * DAY_MILLIS;
        double offset = query.utcOffsetHours();
        LongFunction<String> time = ms -> (ms < earliest || ms > latest) ? null : format(ms, offset);
        // The Duha window runs from sunrise to just before Dhuhr. Without a
        // sunrise there is no window — its end alone (Dhuhr - 10 min) would
        // still compute, but a window with no start means nothing.
        boolean hasDuha = time.apply(result.sunrise()) != null;
        return new PrayerTimes(
            query.date(),
            method.name(),
            query.latitude(),
            query.longitude(),
            query.utcOffsetHours(),
            time.apply(result.fajr()),
            time.apply(result.sunrise()),
            time.apply(result.dhuhr()),
            time.apply(result.asr()),
            time.apply(result.sunset()),
            time.apply(result.maghrib()),
            time.apply(result.isha()),
            time.apply(result.midnight()),
            hasDuha ? time.apply(result.duhaStart()) : null,
            hasDuha ? time.apply(result.duhaEnd()) : null,
            hasDuha ? time.apply(result.duhaBest()) : null);
    }

    private String format(long utcMillis, double utcOffsetHours) {
        return org.openathar.core.PrayerTimes.formatLocalTime(utcMillis, utcOffsetHours);
    }

    /**
     * Method.valueOf wuerde "No enum constant org.openathar.core.Method.X"
     * werfen — ein Java-Interna, das ueber den ExceptionHandler 1:1 beim
     * API-Client landet. Stattdessen: der Wert plus die gueltigen Optionen.
     */
    private static Method parseMethod(String name) {
        try {
            return Method.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            String valid = Arrays.stream(Method.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("unknown method: " + name + " (valid: " + valid + ")");
        }
    }
}
