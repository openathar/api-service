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

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.adapter.web.dto.HijriMonthResponse;
import org.openathar.api.adapter.web.dto.HijriMonthResponse.DayResponse;
import org.openathar.api.adapter.web.dto.HijriResponse;
import org.openathar.api.domain.HijriMonth;
import org.openathar.api.port.in.HijriUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/calendar")
@Tag(name = "Hijri Calendar", description = "Gregorian-to-Hijri (Umm al-Qura) date conversion.")
public class CalendarController {

    private static final CacheControl FOREVER = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final HijriUseCase useCase;
    private final HijriMapper mapper;

    public CalendarController(HijriUseCase useCase, HijriMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Hijri month grid",
        description = """
            One whole Hijri (Umm al-Qura) month, day by day with the Gregorian \
            date and weekday of each — enough to draw a Hijri calendar without \
            shipping conversion logic. Supported years follow the Umm al-Qura \
            table (1300-1600 AH). Cached forever.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The month",
            content = @Content(schema = @Schema(implementation = HijriMonthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid month or a year outside the table",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/hijri")
    public ResponseEntity<HijriMonthResponse> month(
            @Parameter(description = "Hijri year.", example = "1448") @RequestParam int year,
            @Parameter(description = "Hijri month, 1-12.", example = "4") @RequestParam int month,
            @Parameter(description = "Language of the month name.", schema = @Schema(allowableValues = {"en", "ar", "de"}, defaultValue = "en"))
            @RequestParam(defaultValue = "en") String locale) {
        HijriMonth m = useCase.month(year, month, locale);
        var days = m.days().stream().map(d -> new DayResponse(d.day(), d.gregorianDate(), d.weekday().name())).toList();
        return ResponseEntity.ok().cacheControl(FOREVER)
            .body(new HijriMonthResponse(m.year(), m.month(), m.monthName(), m.length(), days));
    }

    @Operation(
        summary = "Hijri to Gregorian",
        description = """
            The reverse conversion: a Hijri (Umm al-Qura) date to its Gregorian \
            date. The answer has the same shape as `GET /v1/hijri`. Cached forever.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The converted date",
            content = @Content(schema = @Schema(implementation = HijriResponse.class))),
        @ApiResponse(responseCode = "400", description = "Not a valid Umm al-Qura date",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping("/gregorian")
    public ResponseEntity<HijriResponse> toGregorian(
            @Parameter(example = "1448") @RequestParam int year,
            @Parameter(description = "1-12.", example = "4") @RequestParam int month,
            @Parameter(description = "1-30.", example = "3") @RequestParam int day,
            @Parameter(schema = @Schema(allowableValues = {"en", "ar", "de"}, defaultValue = "en"))
            @RequestParam(defaultValue = "en") String locale) {
        return ResponseEntity.ok().cacheControl(FOREVER).body(mapper.toResponse(useCase.toGregorian(year, month, day, locale)));
    }
}
