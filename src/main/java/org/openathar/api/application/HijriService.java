package org.openathar.api.application;

import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ValueRange;

import org.openathar.api.domain.HijriDate;
import org.openathar.api.port.in.HijriUseCase;
import org.springframework.stereotype.Service;

/** Delegates Hijri conversion to athan-core-java. */
@Service
public class HijriService implements HijriUseCase {

    /**
     * The core converts via the JDK's Umm al-Qura table, which only covers a
     * fixed span (1300–1600 AH = 1882-11-12 to 2174-11-25). Outside it the JDK
     * throws a DateTimeException that surfaced as a 500. The span is read from
     * the table's year range instead of being hardcoded, so a JDK with a longer
     * table widens it automatically. (Not EPOCH_DAY: its range is the generic
     * ISO span, not the table's.)
     */
    private static final ValueRange YEARS = HijrahChronology.INSTANCE.range(ChronoField.YEAR);
    private static final LocalDate FIRST =
        LocalDate.from(HijrahChronology.INSTANCE.date((int) YEARS.getMinimum(), 1, 1));
    private static final LocalDate LAST = LocalDate.from(
        HijrahChronology.INSTANCE.date((int) YEARS.getMaximum(), 12, 1).with(TemporalAdjusters.lastDayOfMonth()));

    @Override
    public HijriDate convert(LocalDate date, String locale) {
        if (date.isBefore(FIRST) || date.isAfter(LAST)) {
            throw new IllegalArgumentException(
                "date out of supported range: " + date + " (supported: " + FIRST + " to " + LAST + ")");
        }
        org.openathar.core.HijriDate h = org.openathar.core.Hijri.gregorianToHijri(date);
        return new HijriDate(date, h.day(), h.month(), h.year(),
            org.openathar.core.Hijri.monthName(h.month(), locale));
    }
}
