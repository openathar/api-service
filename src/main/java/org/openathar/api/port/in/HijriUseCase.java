package org.openathar.api.port.in;

import java.time.LocalDate;

import org.openathar.api.domain.HijriDate;

/** Use case: convert a Gregorian date to the Hijri (Umm al-Qura) calendar. */
public interface HijriUseCase {

    HijriDate convert(LocalDate date, String locale);
}