package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** REST response for GET /v1/prayer-times. */
@Schema(description = "Prayer times for one location, date and calculation method, as local HH:mm wall-clock strings. Dhuhr is always present; every other time is null where the event does not occur (polar day or night).")
public record PrayerTimesResponse(
        @Schema(description = "The requested date.", example = "2026-09-14") LocalDate date,
        @Schema(description = "Calculation method used.", example = "MWL") String method,
        @Schema(description = "Latitude echoed back.", example = "52.52") double latitude,
        @Schema(description = "Longitude echoed back.", example = "13.405") double longitude,
        @Schema(description = "UTC offset applied to render local wall-clock times.", example = "2") double utcOffsetHours,
        @Schema(example = "04:38", nullable = true, description = "null if the event does not occur on this date at this location (polar day or night).") String fajr,
        @Schema(example = "06:39", nullable = true, description = "null if the event does not occur on this date at this location (polar day or night).") String sunrise,
        @Schema(example = "13:02") String dhuhr,
        @Schema(example = "16:30", nullable = true, description = "null if the event does not occur on this date at this location (polar day or night).") String asr,
        @Schema(example = "19:24", nullable = true, description = "null if the event does not occur on this date at this location (polar day or night).") String sunset,
        @Schema(example = "19:25", nullable = true, description = "null if the event does not occur on this date at this location (polar day or night).") String maghrib,
        @Schema(example = "21:17", nullable = true, description = "null if the event does not occur on this date at this location (polar day or night).") String isha,
        @Schema(example = "01:02", nullable = true, description = "Islamic midnight. Standard methods: solar midnight, always present. TEHRAN/JAFARI: midpoint of sunset and Fajr, null where either does not occur.") String midnight,
        @Schema(description = "Start of the Duha window. Null when there is no sunrise.", nullable = true, example = "06:54") String duhaStart,
        @Schema(description = "End of the Duha window. Null when there is no sunrise.", nullable = true, example = "12:52") String duhaEnd,
        @Schema(description = "Recommended (best) time within the Duha window. Null when there is no sunrise.", nullable = true, example = "09:51") String duhaBest) {
}
