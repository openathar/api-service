package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** REST response for GET /v1/methods. */
@Schema(description = "Every calculation method the API accepts in the `method` parameter, with its parameters.")
public record MethodsResponse(
        List<MethodResponse> methods,
        @Schema(description = "Asr shadow rule used by all methods (the Shafi'i/Maliki/Hanbali rule: shadow = object length).", example = "STANDARD") String asr,
        @Schema(description = "How Fajr and Isha are placed where the sun does not sink far enough.", example = "NIGHT_MIDDLE") String highLatitude) {

    @Schema(description = "One calculation method.")
    public record MethodResponse(
            @Schema(description = "Value for the `method` parameter.", example = "MWL") String id,
            @Schema(example = "Muslim World League") String name,
            @Schema(description = "Sun depression angle for Fajr, in degrees.", example = "18") double fajrAngle,
            @Schema(description = "Isha: an angle in degrees, or minutes after Maghrib.") ThresholdResponse isha,
            @Schema(description = "Maghrib: minutes after sunset, or an angle in degrees.") ThresholdResponse maghrib,
            @Schema(description = "`STANDARD` (solar midnight) or `JAFARI` (midpoint of sunset and Fajr).", example = "STANDARD") String midnight) {
    }

    @Schema(description = "A threshold given either as an angle or as minutes.")
    public record ThresholdResponse(
            @Schema(allowableValues = {"angle", "minutes"}, example = "angle") String type,
            @Schema(example = "17") double value) {
    }
}
