package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.MoonInfo;

class MoonServiceTest {

    private final MoonService service = new MoonService();

    private static void assertWithin(Instant expected, Instant actual, long minutes) {
        long diff = Math.abs(Duration.between(expected, actual).toMinutes());
        assertTrue(diff <= minutes, "expected " + expected + " but was " + actual + " (off by " + diff + " min)");
    }

    /** Reference lunations (USNO): new moon 2000-01-06 18:14 UTC, 2024-10-02 18:49 UTC; full moon 2024-09-18 02:34 UTC. */
    @Test
    void trueNewAndFullMoonsMatchReferenceTimes() {
        MoonInfo a = service.at(Instant.parse("2000-01-20T00:00:00Z"));
        assertWithin(Instant.parse("2000-01-06T18:14:00Z"), a.previousNewMoon(), 5);
        MoonInfo b = service.at(Instant.parse("2024-09-20T00:00:00Z"));
        assertWithin(Instant.parse("2024-10-02T18:49:00Z"), b.nextNewMoon(), 5);
        MoonInfo c = service.at(Instant.parse("2024-09-10T00:00:00Z"));
        assertWithin(Instant.parse("2024-09-18T02:34:00Z"), c.nextFullMoon(), 5);
    }

    @Test
    void phaseAtKnownMomentsIsConsistent() {
        MoonInfo newMoon = service.at(Instant.parse("2024-10-02T19:30:00Z"));
        assertEquals("new", newMoon.phase());
        assertTrue(newMoon.illumination() < 0.01);
        MoonInfo full = service.at(Instant.parse("2024-09-18T03:00:00Z"));
        assertEquals("full", full.phase());
        assertTrue(full.illumination() > 0.99);
        MoonInfo quarter = service.at(Instant.parse("2024-09-24T19:00:00Z"));
        assertEquals("last-quarter", quarter.phase());
        assertEquals(0.5, quarter.illumination(), 0.08);
    }

    @Test
    void ageAndNextEventsAreOrdered() {
        MoonInfo m = service.at(Instant.parse("2026-09-14T15:30:00Z"));
        assertTrue(m.ageDays() >= 0 && m.ageDays() < 29.6);
        assertTrue(m.previousNewMoon().isBefore(m.at()));
        assertTrue(m.nextNewMoon().isAfter(m.at()));
        assertTrue(m.nextFullMoon().isAfter(m.at()));
        assertTrue(Duration.between(m.at(), m.nextFullMoon()).toDays() <= 30);
    }
}
