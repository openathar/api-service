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
import org.openathar.api.adapter.web.dto.EventsResponse;
import org.openathar.api.adapter.web.dto.EventsResponse.EventResponse;
import org.openathar.api.port.in.EventsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
@Tag(name = "Hijri Calendar", description = "Gregorian-to-Hijri (Umm al-Qura) date conversion.")
public class EventsController {

    private final EventsUseCase useCase;

    public EventsController(EventsUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
        summary = "Upcoming Islamic observances",
        description = """
            The next observances from a date, chronologically, each with its \
            Hijri date, Gregorian date and a countdown in days: Islamic New Year, \
            Ashura, Mawlid, start of Ramadan, Laylat al-Qadr, Eid al-Fitr and \
            Eid al-Adha. Pure calendar facts from the Umm al-Qura calendar — a \
            local moon sighting may differ by a day. With an explicit `from` the \
            response is cached forever; without it ("today") for an hour.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The observances",
            content = @Content(schema = @Schema(implementation = EventsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping
    public ResponseEntity<EventsResponse> getEvents(
            @Parameter(description = "Count days from here, ISO-8601 (yyyy-MM-dd). Defaults to today (UTC).", example = "2026-09-14")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "How many observances (1-20).", schema = @Schema(defaultValue = "5"))
            @RequestParam(defaultValue = "5") int count,
            @Parameter(description = "Language of names and notes.", schema = @Schema(allowableValues = {"en", "ar", "de"}, defaultValue = "en"))
            @RequestParam(defaultValue = "en") String locale) {
        LocalDate start = from != null ? from : LocalDate.now(ZoneOffset.UTC);
        var events = useCase.upcoming(start, count, locale).stream()
            .map(e -> new EventResponse(e.key(), e.name(), e.note(), e.hijriDay(), e.hijriMonth(), e.hijriYear(),
                e.gregorianDate(), e.daysUntil()))
            .toList();
        CacheControl cache = from != null
            ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
            : CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
        return ResponseEntity.ok().cacheControl(cache).body(new EventsResponse(start, locale, events));
    }
}
