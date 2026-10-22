package org.openathar.api.port.in;

import java.time.LocalDate;

import org.openathar.api.domain.HijriDate;
import org.openathar.api.domain.HijriMonth;

/** Use case: convert a Gregorian date to the Hijri (Umm al-Qura) calendar. */
public interface HijriUseCase {

    HijriDate convert(LocalDate date, String locale);

    /** A whole Hijri month, day by day with Gregorian dates. */
    HijriMonth month(int year, int month, String locale);

    /** Hijri date to Gregorian (the result is a {@link HijriDate}, same shape as {@link #convert}). */
    HijriDate toGregorian(int year, int month, int day, String locale);
}