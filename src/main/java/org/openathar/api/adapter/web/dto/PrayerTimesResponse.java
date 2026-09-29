package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** REST response for GET /v1/prayer-times. */
@Schema(description = "Prayer times for one location, date and calculation method, as local HH:mm wall-clock strings.")
public record PrayerTimesResponse(
        @Schema(description = "The requested date.", example = "2026-09-14") LocalDate date,
        @Schema(description = "Calculation method used.", example = "MWL") String method,
        @Schema(description = "Latitude echoed back.", example = "52.52") double latitude,
        @Schema(description = "Longitude echoed back.", example = "13.405") double longitude,
        @Schema(description = "UTC offset applied to render local wall-clock times.", example = "2") double utcOffsetHours,
        @Schema(example = "05:09") String fajr,
        @Schema(example = "06:52") String sunrise,
        @Schema(example = "12:57") String dhuhr,
        @Schema(example = "16:04") String asr,
        @Schema(example = "18:49") String sunset,
        @Schema(example = "18:49") String maghrib,
        @Schema(example = "20:36") String isha,
        @Schema(example = "00:00") String midnight,
        @Schema(description = "Start of the Duha window.", example = "07:20") String duhaStart,
        @Schema(description = "End of the Duha window.", example = "12:20") String duhaEnd,
        @Schema(description = "Recommended (best) time within the Duha window.", example = "09:50") String duhaBest) {
}
