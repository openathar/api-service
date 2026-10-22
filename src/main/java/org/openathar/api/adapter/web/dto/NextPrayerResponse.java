package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** REST response for GET /v1/prayer-times/next. */
@Schema(description = "The prayer that is current at the given moment, the next one, and the seconds until it starts. All wall-clock values are in the location's local time (the given UTC offset).")
public record NextPrayerResponse(
        @Schema(example = "MWL") String method,
        @Schema(example = "52.52") double latitude,
        @Schema(example = "13.405") double longitude,
        @Schema(example = "2") double utcOffsetHours,
        @Schema(description = "The moment the answer is for, as local date-time.", example = "2026-09-14T17:30:00") LocalDateTime localTime,
        @Schema(description = "The latest prayer that has already started (can be yesterday's Isha before Fajr).") PrayerEventResponse current,
        @Schema(description = "The next prayer to start (can be tomorrow's Fajr after Isha).") PrayerEventResponse next,
        @Schema(description = "Seconds from `localTime` until `next` starts.", example = "3300") long secondsUntilNext) {

    @Schema(description = "One prayer: its name (`fajr`, `dhuhr`, `asr`, `maghrib` or `isha`), local date and HH:mm time.")
    public record PrayerEventResponse(
            @Schema(example = "asr") String name,
            @Schema(example = "2026-09-14") LocalDate date,
            @Schema(example = "16:30") String time) {
    }
}
