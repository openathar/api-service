package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.NextPrayer;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.domain.PrayerTimesRange;

class PrayerTimesRangeNextTest {

    private final PrayerTimesService service = new PrayerTimesService();

    @Test
    void rangeMatchesSingleDayCalls() {
        PrayerTimesRange r = service.range(52.52, 13.405, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 16), "MWL", 2.0);
        assertEquals(3, r.days().size());
        assertEquals("MWL", r.method());
        for (PrayerTimes day : r.days()) {
            assertEquals(service.calculate(new PrayerTimesQuery(52.52, 13.405, day.date(), "MWL", 2.0)), day);
        }
        assertEquals(LocalDate.of(2026, 9, 14), r.days().get(0).date());
        assertEquals(LocalDate.of(2026, 9, 16), r.days().get(2).date());
    }

    @Test
    void rangeOfOneDayIsAllowedAndOverlongOrReversedRangesAreRejected() {
        assertEquals(1, service.range(52.52, 13.405, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 14), "MWL", 2.0).days().size());
        assertEquals(62, service.range(52.52, 13.405, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "MWL", 2.0).days().size());
        IllegalArgumentException tooLong = assertThrows(IllegalArgumentException.class,
            () -> service.range(52.52, 13.405, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 2), "MWL", 2.0));
        assertEquals("range too long: 63 days (max 62)", tooLong.getMessage());
        assertThrows(IllegalArgumentException.class,
            () -> service.range(52.52, 13.405, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 4), "MWL", 2.0));
    }

    /** Berlin 2026-09-14, UTC+2: Fajr 04:38, Dhuhr 13:02, Asr 16:30, Maghrib 19:25, Isha 21:17. */
    @Test
    void nextBetweenAsrAndMaghrib() {
        // 15:30 UTC = 17:30 local
        NextPrayer n = service.next(52.52, 13.405, "MWL", 2.0, Instant.parse("2026-09-14T15:30:00Z"));
        assertEquals("asr", n.current().name());
        assertEquals("16:30", n.current().time());
        assertEquals("maghrib", n.next().name());
        assertEquals("19:25", n.next().time());
        assertEquals(LocalDate.of(2026, 9, 14), n.next().date());
        assertEquals(115 * 60, n.secondsUntilNext());
    }

    @Test
    void afterIshaTheNextPrayerIsTomorrowsFajr() {
        // 22:00 UTC = 00:00 local on the 15th, which is past Isha of the 14th
        NextPrayer n = service.next(52.52, 13.405, "MWL", 2.0, Instant.parse("2026-09-14T22:00:00Z"));
        assertEquals("isha", n.current().name());
        assertEquals(LocalDate.of(2026, 9, 14), n.current().date());
        assertEquals("fajr", n.next().name());
        assertEquals(LocalDate.of(2026, 9, 15), n.next().date());
        assertTrue(n.secondsUntilNext() > 0 && n.secondsUntilNext() < 6 * 3600);
    }

    @Test
    void beforeFajrTheCurrentPrayerIsYesterdaysIsha() {
        // 01:30 UTC = 03:30 local on the 14th
        NextPrayer n = service.next(52.52, 13.405, "MWL", 2.0, Instant.parse("2026-09-14T01:30:00Z"));
        assertEquals("isha", n.current().name());
        assertEquals(LocalDate.of(2026, 9, 13), n.current().date());
        assertEquals("fajr", n.next().name());
        assertEquals("04:38", n.next().time());
        assertEquals(68 * 60, n.secondsUntilNext());
    }

    @Test
    void exactlyAtAPrayerTimeThatPrayerIsCurrent() {
        // 14:30 UTC = 16:30 local = Asr
        NextPrayer n = service.next(52.52, 13.405, "MWL", 2.0, Instant.parse("2026-09-14T14:30:00Z"));
        assertEquals("asr", n.current().name());
        assertEquals("maghrib", n.next().name());
    }

    @Test
    void nextValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> service.next(91, 13.405, "MWL", 2.0, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> service.next(52.52, 13.405, "FOO", 2.0, Instant.now()));
    }

    /** Polar night/day: missing events are skipped, Dhuhr always exists, so there is still an answer. */
    @Test
    void nextWorksWhereSomePrayersDoNotOccur() {
        NextPrayer n = service.next(78.22, 15.65, "MWL", 1.0, Instant.parse("2026-06-21T10:00:00Z"));
        assertTrue(n.secondsUntilNext() > 0);
    }
}
