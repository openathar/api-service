package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** REST response for GET /v1/qibla. */
@Schema(description = "Qibla bearing from true north for the given location.")
public record QiblaResponse(
        @Schema(description = "Latitude echoed back, in decimal degrees.", example = "52.52") double latitude,
        @Schema(description = "Longitude echoed back, in decimal degrees.", example = "13.405") double longitude,
        @Schema(description = "Bearing to the Kaaba, clockwise from true north, in degrees.", example = "136.68")
        double bearingDegrees) {
}
