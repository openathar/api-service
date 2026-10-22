package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** REST response for GET /v1/places. */
@Schema(description = "Cities matching a name, best match first, each with the coordinates and UTC offset needed for the prayer-time endpoints.")
public record PlacesResponse(
        @Schema(example = "berlin") String query,
        @Schema(description = "Attribution required by the data licence.", example = "GeoNames (CC BY 4.0), https://www.geonames.org/") String source,
        List<PlaceResponse> places) {

    @Schema(description = "One city.")
    public record PlaceResponse(
            @Schema(example = "Berlin") String name,
            @Schema(description = "ISO 3166-1 alpha-2 country code.", example = "DE") String country,
            @Schema(example = "52.52437") double latitude,
            @Schema(example = "13.41053") double longitude,
            @Schema(description = "IANA time zone.", example = "Europe/Berlin") String timezone,
            @Schema(description = "UTC offset in hours on the requested date, daylight saving included. Use as `utcOffset`.", example = "2") double utcOffsetHours,
            @Schema(example = "3426354") long population) {
    }
}
