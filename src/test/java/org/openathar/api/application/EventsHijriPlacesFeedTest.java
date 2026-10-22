package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.BatchLocation;
import org.openathar.api.domain.HijriDate;
import org.openathar.api.domain.HijriMonth;
import org.openathar.api.domain.IslamicEvent;
import org.openathar.api.domain.Place;
import org.openathar.api.domain.PrayerTimes;

class EventsHijriPlacesFeedTest {

    private final EventsService events = new EventsService();
    private final HijriService hijri = new HijriService();
    private final PlacesService places = new PlacesService();
    private final PrayerTimesService prayer = new PrayerTimesService();

    @Test
    void eventsAreChronologicalWithCountdowns() {
        List<IslamicEvent> list = events.upcoming(LocalDate.of(2026, 9, 14), 5, "en");
        assertEquals(5, list.size());
        for (int i = 1; i < list.size(); i++) {
            assertFalse(list.get(i).gregorianDate().isBefore(list.get(i - 1).gregorianDate()));
        }
        IslamicEvent first = list.get(0);
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2026, 9, 14), first.gregorianDate()), first.daysUntil());
        assertTrue(first.daysUntil() >= 0);
    }

    @Test
    void eventOnTheDayItselfHasZeroDaysAndNamesFollowLocale() {
        LocalDate eid = events.upcoming(LocalDate.of(2026, 9, 14), 20, "en").stream()
            .filter(e -> e.key().equals("eid-al-fitr")).findFirst().orElseThrow().gregorianDate();
        IslamicEvent e = events.upcoming(eid, 1, "ar").get(0);
        assertEquals("eid-al-fitr", e.key());
        assertEquals(0, e.daysUntil());
        assertEquals("عيد الفطر", e.name());
        assertEquals(1, e.hijriDay());
        assertEquals(10, e.hijriMonth());
    }

    @Test
    void eventsValidateInput() {
        assertThrows(IllegalArgumentException.class, () -> events.upcoming(LocalDate.of(2026, 9, 14), 0, "en"));
        assertThrows(IllegalArgumentException.class, () -> events.upcoming(LocalDate.of(2026, 9, 14), 21, "en"));
        assertThrows(IllegalArgumentException.class, () -> events.upcoming(LocalDate.of(2026, 9, 14), 3, "fr"));
    }

    /** 2026-09-14 is 3 Rabi' al-thani 1448 (see the /v1/hijri reference). */
    @Test
    void hijriMonthGridAndReverseConversionAgree() {
        HijriMonth m = hijri.month(1448, 4, "en");
        assertEquals("Rabi' al-thani", m.monthName());
        assertTrue(m.length() == 29 || m.length() == 30);
        assertEquals(m.length(), m.days().size());
        assertEquals(LocalDate.of(2026, 9, 14), m.days().get(2).gregorianDate());
        HijriDate back = hijri.toGregorian(1448, 4, 3, "en");
        assertEquals(LocalDate.of(2026, 9, 14), back.gregorianDate());
        assertEquals(hijri.convert(LocalDate.of(2026, 9, 14), "en").day(), 3);
    }

    @Test
    void hijriRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> hijri.month(1448, 13, "en"));
        assertThrows(IllegalArgumentException.class, () -> hijri.month(1000, 1, "en"));
        assertThrows(IllegalArgumentException.class, () -> hijri.toGregorian(1448, 4, 31, "en"));
    }

    @Test
    void placesFindCitiesAcrossScriptsAndResolveDaylightSaving() {
        Place berlin = places.search("Berlin", 3, LocalDate.of(2026, 7, 1)).get(0);
        assertEquals("Europe/Berlin", berlin.timezone());
        assertEquals(2.0, berlin.utcOffsetHours());
        assertEquals(1.0, places.search("berlin", 1, LocalDate.of(2026, 1, 15)).get(0).utcOffsetHours());
        assertEquals("Makkah", places.search("mecca", 1, null).get(0).name());
        assertFalse(places.search("مكة", 5, null).isEmpty());
        assertEquals(5.5, places.search("new delhi", 1, null).get(0).utcOffsetHours());
        assertTrue(places.search("qqqqzzzz", 5, null).isEmpty());
    }

    @Test
    void placesAccentInsensitiveAndValidated() {
        assertFalse(places.search("münchen", 1, null).isEmpty());
        assertFalse(places.search("Munchen", 1, null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> places.search("a", 5, null));
        assertThrows(IllegalArgumentException.class, () -> places.search("berlin", 100, null));
    }

    @Test
    void feedIsAValidCalendarWithUtcTimes() {
        CalendarFeedService feed = new CalendarFeedService(prayer);
        String ics = feed.prayerTimes(52.52, 13.405, LocalDate.of(2026, 9, 14), 1, "MWL", 2.0, "en", false);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertEquals(5, ics.split("BEGIN:VEVENT", -1).length - 1);
        // Fajr 04:38 local at UTC+2 is 02:38Z
        assertTrue(ics.contains("DTSTART:20260914T023800Z\r\n"));
        assertTrue(ics.contains("SUMMARY:Fajr\r\n"));
        assertTrue(ics.contains("DTEND:20260914T024800Z\r\n"));
        assertFalse(ics.contains("Sunrise"));
        assertTrue(feed.prayerTimes(52.52, 13.405, LocalDate.of(2026, 9, 14), 1, "MWL", 2.0, "ar", true).contains("SUMMARY:الشروق"));
        assertThrows(IllegalArgumentException.class, () -> feed.prayerTimes(52.52, 13.405, LocalDate.of(2026, 9, 14), 63, "MWL", 2.0, "en", false));
        assertThrows(IllegalArgumentException.class, () -> feed.prayerTimes(52.52, 13.405, LocalDate.of(2026, 9, 14), 1, "MWL", 2.0, "fr", false));
    }

    @Test
    void batchKeepsOrderAndNamesTheBadIndex() {
        List<PrayerTimes> out = prayer.batch(List.of(
            new BatchLocation("a", 52.52, 13.405, 2.0), new BatchLocation("b", 21.4225, 39.8262, 3.0)),
            LocalDate.of(2026, 9, 14), "MWL");
        assertEquals(2, out.size());
        assertEquals("04:38", out.get(0).fajr());
        assertEquals(21.4225, out.get(1).latitude());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> prayer.batch(List.of(
            new BatchLocation("a", 52.52, 13.405, 2.0), new BatchLocation("b", 95, 0, 0)), LocalDate.of(2026, 9, 14), "MWL"));
        assertTrue(e.getMessage().startsWith("locations[1]:"));
        assertThrows(IllegalArgumentException.class, () -> prayer.batch(List.of(), LocalDate.of(2026, 9, 14), "MWL"));
        List<BatchLocation> many = java.util.Collections.nCopies(51, new BatchLocation(null, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> prayer.batch(many, LocalDate.of(2026, 9, 14), "MWL"));
    }
}
