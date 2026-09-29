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
import org.openathar.api.adapter.web.dto.PrayerTimesResponse;
import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.port.in.PrayerTimesUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prayer-times")
@Tag(name = "Prayer Times", description = "Astronomical prayer-time calculation for any location, date and method.")
public class PrayerTimesController {

    private final PrayerTimesUseCase useCase;
    private final PrayerTimesMapper mapper;

    public PrayerTimesController(PrayerTimesUseCase useCase, PrayerTimesMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Calculate prayer times",
        description = """
            Returns Fajr, Dhuhr, Asr, Maghrib and Isha — plus sunrise, sunset, \
            midnight and the Duha window — as local HH:mm wall-clock strings \
            for the given location, date and calculation method. Deterministic \
            for a given input: safe to cache forever (see `Cache-Control`).""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prayer times calculated successfully",
            content = @Content(schema = @Schema(implementation = PrayerTimesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters, e.g. out-of-range latitude or an unknown method",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping
    public ResponseEntity<?> getPrayerTimes(
            @Parameter(description = "Latitude in decimal degrees.", example = "52.52")
            @RequestParam double lat,
            @Parameter(description = "Longitude in decimal degrees.", example = "13.405")
            @RequestParam double lon,
            @Parameter(description = "Date in ISO-8601 (yyyy-MM-dd).", example = "2026-09-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(
                description = "Calculation method.",
                schema = @Schema(allowableValues = {
                    "MWL", "ISNA", "EGYPT", "MAKKAH", "KARACHI", "TEHRAN", "JAFARI", "FRANCE", "RUSSIA",
                    "MALAYSIA", "SINGAPORE"
                }, defaultValue = "MWL"))
            @RequestParam(defaultValue = "MWL") String method,
            @Parameter(description = "Location's UTC offset in hours (-12..14), used to render local wall-clock times.", example = "2")
            @RequestParam(defaultValue = "0") double utcOffset) {
        PrayerTimesQuery query = new PrayerTimesQuery(lat, lon, date, method, utcOffset);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(mapper.toResponse(useCase.calculate(query)));
    }
}
