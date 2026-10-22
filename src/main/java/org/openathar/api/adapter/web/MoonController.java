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
import java.time.Instant;

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.adapter.web.dto.MoonResponse;
import org.openathar.api.domain.MoonInfo;
import org.openathar.api.port.in.MoonUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/moon")
@Tag(name = "Astronomy", description = "Moon phase and lunation events.")
public class MoonController {

    private final MoonUseCase useCase;

    public MoonController(MoonUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
        summary = "Moon phase and events",
        description = """
            Phase, illuminated fraction, age and the next new and full moon at an \
            instant (default: now). Calculated from the true times of new and full \
            moon, accurate to a couple of minutes. The astronomy beneath the Hijri \
            month — but not a moon sighting: a month begins with the observed \
            crescent, not with the conjunction. With `at` the answer is cached \
            forever; without it, it is not cached.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Moon state",
            content = @Content(schema = @Schema(implementation = MoonResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping
    public ResponseEntity<MoonResponse> getMoon(
            @Parameter(description = "ISO-8601 instant. Defaults to now.", example = "2026-09-14T15:30:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        MoonInfo m = useCase.at(at != null ? at : Instant.now());
        CacheControl cache = at != null
            ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
            : CacheControl.noStore();
        return ResponseEntity.ok().cacheControl(cache).body(new MoonResponse(m.at().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
            m.phase(), round(m.illumination(), 4), round(m.ageDays(), 2), round(m.phaseAngleDegrees(), 1),
            m.previousNewMoon().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
            m.nextNewMoon().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
            m.nextFullMoon().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)));
    }

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }
}
