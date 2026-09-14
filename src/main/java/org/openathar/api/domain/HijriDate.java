package org.openathar.api.domain;

import java.time.LocalDate;

/** Hijri (Umm al-Qura) date for a Gregorian date, with localized month name. */
public record HijriDate(LocalDate gregorianDate, int day, int month, int year, String monthName) {
}