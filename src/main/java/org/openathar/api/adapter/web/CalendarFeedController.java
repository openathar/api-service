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
import java.time.ZoneOffset;

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.port.in.CalendarFeedUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Prayer Times", description = "Astronomical prayer-time calculation for any location, date and method.")
public class CalendarFeedController {

    private static final MediaType CALENDAR = new MediaType("text", "calendar", java.nio.charset.StandardCharsets.UTF_8);

    private final CalendarFeedUseCase useCase;

    public CalendarFeedController(CalendarFeedUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
        summary = "Prayer times as a calendar feed (iCalendar)",
        description = """
            An iCalendar (`text/calendar`) feed of prayer times that Apple \\
            Calendar, Google Calendar and Outlook can subscribe to by URL. One \\
            short event per prayer (optionally sunrise), from `from` (default \\
            today) for `days` days (default 30, max 62). Event times are in UTC, so \\
            they are right on every device. Calendar apps re-fetch the feed on \\
            their own; since the window starts today it keeps rolling forward. \\
            With an explicit `from` the feed is cached forever, otherwise for an hour.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The feed",
            content = @Content(mediaType = "text/calendar", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping(value = "/v1/prayer-times.ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<String> feed(
            @Parameter(example = "52.52") @RequestParam double lat,
            @Parameter(example = "13.405") @RequestParam double lon,
            @Parameter(description = "First day (yyyy-MM-dd). Defaults to today (UTC).", example = "2026-09-14")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Number of days (1-62).", schema = @Schema(defaultValue = "30")) @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Calculation method (see `GET /v1/methods`).", schema = @Schema(defaultValue = "MWL"))
            @RequestParam(defaultValue = "MWL") String method,
            @Parameter(description = "Location's UTC offset in hours (-12..14).", example = "2") @RequestParam(defaultValue = "0") double utcOffset,
            @Parameter(description = "Language of the event names.", schema = @Schema(allowableValues = {"en", "de", "ar"}, defaultValue = "en"))
            @RequestParam(defaultValue = "en") String lang,
            @Parameter(description = "Also add a sunrise event.", schema = @Schema(defaultValue = "false"))
            @RequestParam(defaultValue = "false") boolean sunrise) {
        LocalDate start = from != null ? from : LocalDate.now(ZoneOffset.UTC);
        String body = useCase.prayerTimes(lat, lon, start, days, method, utcOffset, lang, sunrise);
        CacheControl cache = from != null
            ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
            : CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
        return ResponseEntity.ok().contentType(CALENDAR).cacheControl(cache).body(body);
    }
}
