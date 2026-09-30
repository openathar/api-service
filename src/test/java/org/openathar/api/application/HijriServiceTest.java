package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** Ausserhalb der Umm-al-Qura-Tabelle warf der Kern eine DateTimeException — das wurde ein 500er. */
    @Test
    void dateOutsideUmmAlQuraTableIsABadRequestNotACrash() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> service.convert(java.time.LocalDate.of(1800, 1, 1), "en"));
        assertTrue(e.getMessage().startsWith("date out of supported range: 1800-01-01 (supported: "), e.getMessage());
    }
}
