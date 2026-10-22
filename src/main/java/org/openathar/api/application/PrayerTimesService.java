package org.openathar.api.application;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.Collectors;

import org.openathar.api.domain.BatchLocation;
import org.openathar.api.domain.NextPrayer;
import org.openathar.api.domain.PrayerEvent;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesRange;
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
    /** Longest range one call may ask for (about two months — covers any monthly calendar). */
    static final int MAX_RANGE_DAYS = 62;
    private static final List<String> PRAYERS = List.of("fajr", "dhuhr", "asr", "maghrib", "isha");

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

    @Override
    public PrayerTimesRange range(double lat, double lon, LocalDate from, LocalDate to, String method, double utcOffsetHours) {
        if (to.isBefore(from)) throw new IllegalArgumentException("'to' must not be before 'from'");
        long count = ChronoUnit.DAYS.between(from, to) + 1;
        if (count > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("range too long: " + count + " days (max " + MAX_RANGE_DAYS + ")");
        }
        List<PrayerTimes> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(calculate(new PrayerTimesQuery(lat, lon, d, method, utcOffsetHours)));
        }
        return new PrayerTimesRange(from, to, parseMethod(method).name(), lat, lon, utcOffsetHours, days);
    }

    /** Upper bound per call: keeps one request cheap enough to count as one against the rate limit. */
    static final int MAX_BATCH = 50;

    @Override
    public List<PrayerTimes> batch(List<BatchLocation> locations, LocalDate date, String method) {
        if (locations == null || locations.isEmpty()) throw new IllegalArgumentException("locations must not be empty");
        if (locations.size() > MAX_BATCH) {
            throw new IllegalArgumentException("too many locations: " + locations.size() + " (max " + MAX_BATCH + ")");
        }
        List<PrayerTimes> out = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            BatchLocation l = locations.get(i);
            try {
                out.add(calculate(new PrayerTimesQuery(l.latitude(), l.longitude(), date, method, l.utcOffsetHours())));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("locations[" + i + "]: " + e.getMessage());
            }
        }
        return out;
    }

    @Override
    public NextPrayer next(double lat, double lon, String method, double utcOffsetHours, Instant at) {
        // Validate the same way as the other endpoints (throws IllegalArgumentException).
        new PrayerTimesQuery(lat, lon, LocalDate.EPOCH, method, utcOffsetHours);
        LocalDateTime now = LocalDateTime.ofInstant(at, ZoneOffset.ofTotalSeconds((int) Math.round(utcOffsetHours * 3600)))
            .truncatedTo(ChronoUnit.SECONDS);
        record Slot(String name, LocalDateTime at) {
        }
        List<Slot> slots = new ArrayList<>();
        for (int shift = -1; shift <= 1; shift++) {
            LocalDate day = now.toLocalDate().plusDays(shift);
            PrayerTimes t = calculate(new PrayerTimesQuery(lat, lon, day, method, utcOffsetHours));
            // Times are HH:mm only: a time that is earlier than the one before it
            // (an Isha past midnight at high latitudes) belongs to the next date.
            LocalDate date = day;
            LocalTime prev = null;
            for (String name : PRAYERS) {
                String value = switch (name) {
                    case "fajr" -> t.fajr();
                    case "dhuhr" -> t.dhuhr();
                    case "asr" -> t.asr();
                    case "maghrib" -> t.maghrib();
                    default -> t.isha();
                };
                if (value == null) continue;
                LocalTime time = LocalTime.parse(value);
                if (prev != null && time.isBefore(prev)) date = date.plusDays(1);
                prev = time;
                slots.add(new Slot(name, LocalDateTime.of(date, time)));
            }
        }
        slots.sort(Comparator.comparing(Slot::at));
        Slot current = null;
        Slot next = null;
        for (Slot s : slots) {
            if (!s.at().isAfter(now)) current = s;
            else if (next == null) next = s;
        }
        if (current == null || next == null) throw new IllegalStateException("no prayer found around " + now);
        return new NextPrayer(parseMethod(method).name(), lat, lon, utcOffsetHours, now,
            event(current.name(), current.at()), event(next.name(), next.at()),
            Duration.between(now, next.at()).getSeconds());
    }

    private static PrayerEvent event(String name, LocalDateTime at) {
        return new PrayerEvent(name, at.toLocalDate(), at.toLocalTime().toString());
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
