package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    /** Die Meldung geht 1:1 an API-Clients — keine Java-Klassennamen, dafuer die gueltigen Werte. */
    @Test
    void unknownMethodNamesValidOptions() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            service.calculate(new PrayerTimesQuery(52.52, 13.405, LocalDate.of(2026, 9, 14), "FOO", 2.0)));
        assertEquals("unknown method: FOO (valid: MWL, ISNA, EGYPT, MAKKAH, KARACHI, TEHRAN, JAFARI, FRANCE, RUSSIA, MALAYSIA, SINGAPORE)",
            e.getMessage());
    }

    /**
     * Mitternachtssonne in Tromsoe: Sonnenauf- und -untergang finden nicht
     * statt. Der Kern liefert dafuer NaN -> Epoch 0, was frueher als "02:00"
     * herauskam. Richtig ist: kein Wert statt eines erfundenen.
     */
    @Test
    void eventsThatDoNotOccurAreNullNotFakeTimes() {
        PrayerTimes t = service.calculate(new PrayerTimesQuery(69.65, 18.96, LocalDate.of(2026, 6, 21), "MWL", 2.0));
        assertNull(t.sunrise());
        assertNull(t.sunset());
        assertNull(t.maghrib());
        assertNull(t.fajr());
        assertNull(t.isha());
        assertNull(t.duhaStart());
        assertNull(t.duhaBest());
        assertNull(t.duhaEnd(), "a Duha window without a start is no window");
        assertNotNull(t.dhuhr());
        assertEquals("12:46", t.dhuhr());
    }
}
