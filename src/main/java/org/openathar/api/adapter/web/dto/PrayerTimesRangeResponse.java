package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** REST response for GET /v1/prayer-times/range. */
@Schema(description = "Prayer times for every day from `from` to `to` (inclusive) at one location and method. Each entry has the same shape as the response of `GET /v1/prayer-times`.")
public record PrayerTimesRangeResponse(
        @Schema(example = "2026-09-01") LocalDate from,
        @Schema(example = "2026-09-30") LocalDate to,
        @Schema(description = "Calculation method used.", example = "MWL") String method,
        @Schema(example = "52.52") double latitude,
        @Schema(example = "13.405") double longitude,
        @Schema(description = "UTC offset applied to render local wall-clock times.", example = "2") double utcOffsetHours,
        List<PrayerTimesResponse> days) {
}
