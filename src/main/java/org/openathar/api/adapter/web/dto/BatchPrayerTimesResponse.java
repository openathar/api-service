package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** REST response for POST /v1/prayer-times/batch. */
@Schema(description = "Prayer times for every requested location, in request order.")
public record BatchPrayerTimesResponse(
        @Schema(example = "2026-09-14") LocalDate date,
        @Schema(example = "MWL") String method,
        List<Result> results) {

    @Schema(description = "One location's result.")
    public record Result(
            @Schema(description = "The label from the request, or null.", nullable = true) String label,
            PrayerTimesResponse times) {
    }
}
