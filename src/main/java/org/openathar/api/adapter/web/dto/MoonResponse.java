package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** REST response for GET /v1/moon. */
@Schema(description = "Moon state at one instant. Calculated from the true times of new and full moon (accurate to a couple of minutes). This is astronomy — the start of a Hijri month depends on the actual crescent sighting.")
public record MoonResponse(
        @Schema(description = "The instant the answer is for (UTC).", example = "2026-09-14T15:30:00Z") Instant at,
        @Schema(description = "Phase name.", allowableValues = {"new", "waxing-crescent", "first-quarter", "waxing-gibbous", "full", "waning-gibbous", "last-quarter", "waning-crescent"}, example = "waxing-gibbous") String phase,
        @Schema(description = "Illuminated fraction of the disc, 0 (new) to 1 (full).", example = "0.74") double illumination,
        @Schema(description = "Days since the last new moon.", example = "9.8") double ageDays,
        @Schema(description = "Elongation from the sun along the lunation, 0-360 degrees (0 = new, 180 = full).", example = "119.4") double phaseAngleDegrees,
        @Schema(description = "The last new moon (UTC).") Instant previousNewMoon,
        @Schema(description = "The next new moon (UTC).") Instant nextNewMoon,
        @Schema(description = "The next full moon (UTC).") Instant nextFullMoon) {
}
