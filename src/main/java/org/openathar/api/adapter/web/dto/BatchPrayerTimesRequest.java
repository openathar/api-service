package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Request body for POST /v1/prayer-times/batch. */
@Schema(description = "One date and method, many locations (at most 50).")
public record BatchPrayerTimesRequest(
        @Schema(description = "Date, ISO-8601.", example = "2026-09-14") LocalDate date,
        @Schema(description = "Calculation method (default MWL).", example = "MWL") String method,
        List<Location> locations) {

    @Schema(description = "One location.")
    public record Location(
            @Schema(description = "Free text echoed back to match results to your records, e.g. a mosque id.", example = "mosque-17") String label,
            @Schema(example = "52.52") Double lat,
            @Schema(example = "13.405") Double lon,
            @Schema(description = "UTC offset in hours (-12..14); default 0.", example = "2") Double utcOffset) {
    }
}
