package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** REST response for GET /v1/calendar/hijri. */
@Schema(description = "One Hijri (Umm al-Qura) month, day by day with the Gregorian date and weekday of each day.")
public record HijriMonthResponse(
        @Schema(example = "1448") int year,
        @Schema(description = "Month number (1-12).", example = "4") int month,
        @Schema(example = "Rabi' al-thani") String monthName,
        @Schema(description = "Days in this month (29 or 30).", example = "30") int length,
        List<DayResponse> days) {

    @Schema(description = "One day of the month.")
    public record DayResponse(
            @Schema(example = "3") int day,
            @Schema(example = "2026-09-14") LocalDate gregorianDate,
            @Schema(description = "English weekday name.", example = "MONDAY") String weekday) {
    }
}
