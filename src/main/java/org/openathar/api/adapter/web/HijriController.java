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
import org.openathar.api.adapter.web.dto.HijriResponse;
import org.openathar.api.domain.HijriDate;
import org.openathar.api.port.in.HijriUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/hijri")
@Tag(name = "Hijri Calendar", description = "Gregorian-to-Hijri (Umm al-Qura) date conversion.")
public class HijriController {

    private final HijriUseCase useCase;
    private final HijriMapper mapper;

    public HijriController(HijriUseCase useCase, HijriMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Convert a Gregorian date to Hijri",
        description = """
            Converts a Gregorian date to the Hijri (Umm al-Qura) calendar. \
            Defaults to today (UTC) if `date` is omitted; `locale` only affects \
            the localized month name (`de` uses the same transliteration as `en`). \
            Supported range is the span of the Umm al-Qura table, 1882-11-12 to \
            2174-11-25; dates outside it return 400. With an explicit `date` the \
            response is cached forever; without one it means "today" and is \
            cached for 5 minutes only.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Date converted successfully",
            content = @Content(schema = @Schema(implementation = HijriResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters, e.g. a malformed date or one outside the supported range",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping
    public ResponseEntity<?> getHijri(
            @Parameter(description = "Date in ISO-8601 (yyyy-MM-dd). Defaults to today.", example = "2026-09-14")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Locale for the Hijri month name.", schema = @Schema(allowableValues = {"en", "ar", "de"}, defaultValue = "en"))
            @RequestParam(defaultValue = "en") String locale) {
        HijriDate result = useCase.convert(date != null ? date : LocalDate.now(ZoneOffset.UTC), locale);
        // Ein explizites Datum ergibt immer dasselbe Ergebnis -> ewig cachebar.
        // Ohne Datum ist die Antwort "heute" und veraltet um Mitternacht; als
        // immutable gecacht bliebe das Datum vom ersten Abruf ein Jahr stehen.
        CacheControl cache = date != null
            ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
            : CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();
        return ResponseEntity.ok()
            .cacheControl(cache)
            .body(mapper.toResponse(result));
    }
}
