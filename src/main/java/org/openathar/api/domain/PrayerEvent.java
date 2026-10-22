package org.openathar.api.domain;

import java.time.LocalDate;

/** One prayer on one local calendar date (the date can roll over past midnight for a late Isha). */
public record PrayerEvent(String name, LocalDate date, String time) {
}
