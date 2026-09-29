package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** REST response for GET /v1/hijri. */
@Schema(description = "Hijri (Umm al-Qura) date equivalent to the requested Gregorian date.")
public record HijriResponse(
        @Schema(description = "The Gregorian date that was converted.", example = "2026-09-14") LocalDate gregorianDate,
        @Schema(description = "Day of the Hijri month (1-30).", example = "3") int day,
        @Schema(description = "Hijri month number (1-12).", example = "3") int month,
        @Schema(description = "Hijri year.", example = "1448") int year,
        @Schema(description = "Localized Hijri month name (see `locale`).", example = "Rabi' al-awwal") String monthName) {
}
