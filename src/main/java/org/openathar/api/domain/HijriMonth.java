package org.openathar.api.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** One Hijri month laid out day by day with the Gregorian equivalents. */
public record HijriMonth(int year, int month, String monthName, int length, List<Day> days) {

    public record Day(int day, LocalDate gregorianDate, DayOfWeek weekday) {
    }
}
