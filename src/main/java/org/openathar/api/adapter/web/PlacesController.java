package org.openathar.api.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.time.LocalDate;

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.adapter.web.dto.PlacesResponse;
import org.openathar.api.adapter.web.dto.PlacesResponse.PlaceResponse;
import org.openathar.api.port.in.PlacesUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/places")
@Tag(name = "Places", description = "City lookup with coordinates and time zone.")
public class PlacesController {

    static final String SOURCE = "GeoNames (CC BY 4.0), https://www.geonames.org/";

    private final PlacesUseCase useCase;

    public PlacesController(PlacesUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
        summary = "Find a city",
        description = """
            Resolves a city name (Latin or Arabic script, accent-insensitive) to \
            coordinates, IANA time zone and the UTC offset on a date — everything \
            the prayer-time endpoints need, without a third-party geocoder. Covers \
            cities above 15,000 people. Best match first: exact name, then prefix, \
            then contained; larger cities first within a tier. Data: GeoNames, \
            CC BY 4.0. Cached for a day.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching cities (possibly none)",
            content = @Content(schema = @Schema(implementation = PlacesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Query shorter than 2 characters or invalid limit",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping
    public ResponseEntity<PlacesResponse> search(
            @Parameter(description = "City name, at least 2 characters.", example = "berlin") @RequestParam String q,
            @Parameter(description = "Maximum results (1-25).", schema = @Schema(defaultValue = "10")) @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Date for the UTC offset (daylight saving). Defaults to today.", example = "2026-09-14")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var places = useCase.search(q, limit, date).stream()
            .map(p -> new PlaceResponse(p.name(), p.country(), p.latitude(), p.longitude(), p.timezone(), p.utcOffsetHours(), p.population()))
            .toList();
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .body(new PlacesResponse(q, SOURCE, places));
    }
}
