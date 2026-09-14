package org.openathar.api.adapter.web.dto;

import java.time.LocalDate;

/** REST response for GET /v1/hijri. */
public record HijriResponse(LocalDate gregorianDate, int day, int month, int year, String monthName) {
}