package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;

class PrayerTimesServiceTest {

    private final PrayerTimesService service = new PrayerTimesService();

    @Test
    void berlinMwlMatchesCoreReference() {
        PrayerTimes t = service.calculate(new PrayerTimesQuery(52.52, 13.405, LocalDate.of(2026, 9, 14), "MWL", 2.0));
        assertEquals("04:38", t.fajr());
        assertEquals("06:39", t.sunrise());
        assertEquals("13:02", t.dhuhr());
        assertEquals("16:30", t.asr());
        assertEquals("19:24", t.sunset());
        assertEquals("19:25", t.maghrib());
        assertEquals("21:17", t.isha());
        assertEquals("01:02", t.midnight());
        assertEquals("06:54", t.duhaStart());
        assertEquals("12:52", t.duhaEnd());
        assertEquals("09:51", t.duhaBest());
    }

    @Test
    void methodIsCaseInsensitive() {
        PrayerTimes t = service.calculate(new PrayerTimesQuery(52.52, 13.405, LocalDate.of(2026, 9, 14), "mwl", 2.0));
        assertEquals("MWL", t.method());
        assertEquals("04:38", t.fajr());
    }
}