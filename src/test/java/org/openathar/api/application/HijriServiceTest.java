package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.HijriDate;

class HijriServiceTest {

    private final HijriService service = new HijriService();

    @Test
    void convertsGregorianToHijriWithMonthName() {
        HijriDate h = service.convert(LocalDate.of(2026, 9, 14), "en");

        assertEquals(LocalDate.of(2026, 9, 14), h.gregorianDate());
        assertEquals(3, h.day());
        assertEquals(4, h.month());
        assertEquals(1448, h.year());
        assertEquals("Rabi' al-thani", h.monthName());
    }

    @Test
    void unknownLocaleFallsBackToEnglish() {
        HijriDate h = service.convert(LocalDate.of(2026, 9, 14), "xx");

        assertEquals("Rabi' al-thani", h.monthName());
    }
}