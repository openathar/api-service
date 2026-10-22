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
import java.time.LocalDate;

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.adapter.web.dto.NextPrayerResponse;
import org.openathar.api.adapter.web.dto.NextPrayerResponse.PrayerEventResponse;
import org.openathar.api.adapter.web.dto.PrayerTimesRangeResponse;
import org.openathar.api.adapter.web.dto.PrayerTimesResponse;
import org.openathar.api.domain.NextPrayer;
import org.openathar.api.domain.PrayerEvent;
import org.openathar.api.domain.PrayerTimesRange;
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
            for a given input: safe to cache forever (see `Cache-Control`). \
            At high latitudes, when the sun does not sink far enough for Fajr \
            or Isha, both are placed by the middle-of-the-night rule. Where an \
            event does not occur at all (polar day or night), its field is \
            `null` instead of a made-up time.""")
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

    @Operation(
        summary = "Calculate prayer times for a date range",
        description = """
            Every day from `from` to `to` (inclusive, at most 62 days) in one call — \
            a printable month costs one request instead of thirty. Each entry is \
            exactly what `GET /v1/prayer-times` returns for that day. Deterministic: \
            safe to cache forever.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prayer times for every day of the range",
            content = @Content(schema = @Schema(implementation = PrayerTimesRangeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters, `to` before `from`, or more than 62 days",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/range")
    public ResponseEntity<?> getRange(
            @Parameter(example = "52.52") @RequestParam double lat,
            @Parameter(example = "13.405") @RequestParam double lon,
            @Parameter(description = "First day, ISO-8601 (yyyy-MM-dd).", example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day (inclusive), ISO-8601 (yyyy-MM-dd).", example = "2026-09-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Calculation method (see `GET /v1/methods`).", schema = @Schema(defaultValue = "MWL"))
            @RequestParam(defaultValue = "MWL") String method,
            @Parameter(description = "Location's UTC offset in hours (-12..14).", example = "2")
            @RequestParam(defaultValue = "0") double utcOffset) {
        PrayerTimesRange range = useCase.range(lat, lon, from, to, method, utcOffset);
        PrayerTimesRangeResponse body = new PrayerTimesRangeResponse(range.from(), range.to(), range.method(),
            range.latitude(), range.longitude(), range.utcOffsetHours(),
            range.days().stream().map(mapper::toResponse).toList());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(body);
    }

    @Operation(
        summary = "Current and next prayer",
        description = """
            Which prayer is current, which is next, and how many seconds until it \
            starts — the value every prayer app needs and used to rebuild from the \
            daily response. Rolls over midnight correctly: before Fajr the current \
            prayer is yesterday's Isha, after Isha the next one is tomorrow's Fajr. \
            Without `at` the answer is for now and is not cached; with `at` it is \
            deterministic and cached forever.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current and next prayer",
            content = @Content(schema = @Schema(implementation = NextPrayerResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/next")
    public ResponseEntity<?> getNext(
            @Parameter(example = "52.52") @RequestParam double lat,
            @Parameter(example = "13.405") @RequestParam double lon,
            @Parameter(description = "Calculation method (see `GET /v1/methods`).", schema = @Schema(defaultValue = "MWL"))
            @RequestParam(defaultValue = "MWL") String method,
            @Parameter(description = "Location's UTC offset in hours (-12..14).", example = "2")
            @RequestParam(defaultValue = "0") double utcOffset,
            @Parameter(description = "The moment to answer for, as an ISO-8601 instant. Defaults to now.", example = "2026-09-14T15:30:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        NextPrayer n = useCase.next(lat, lon, method, utcOffset, at != null ? at : Instant.now());
        NextPrayerResponse body = new NextPrayerResponse(n.method(), n.latitude(), n.longitude(), n.utcOffsetHours(),
            n.localTime(), event(n.current()), event(n.next()), n.secondsUntilNext());
        CacheControl cache = at != null
            ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
            : CacheControl.noStore();
        return ResponseEntity.ok().cacheControl(cache).body(body);
    }

    private static PrayerEventResponse event(PrayerEvent e) {
        return new PrayerEventResponse(e.name(), e.date(), e.time());
    }
}
