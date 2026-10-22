package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** REST response for GET /v1/events. */
@Schema(description = "Upcoming Islamic observances with a countdown from `from`. Dates follow the Umm al-Qura calendar; a local moon sighting may differ by a day.")
public record EventsResponse(
        @Schema(example = "2026-09-14") LocalDate from,
        @Schema(example = "en") String locale,
        List<EventResponse> events) {

    @Schema(description = "One observance.")
    public record EventResponse(
            @Schema(description = "Stable identifier.", example = "ramadan-start") String key,
            @Schema(example = "Start of Ramadan") String name,
            @Schema(example = "Subject to moon sighting; the calculated date may shift by a day.") String note,
            @Schema(description = "Hijri day of month.", example = "1") int hijriDay,
            @Schema(description = "Hijri month (1-12).", example = "9") int hijriMonth,
            @Schema(description = "Hijri year.", example = "1448") int hijriYear,
            @Schema(example = "2027-02-08") LocalDate gregorianDate,
            @Schema(description = "Days from `from` until the observance (0 = on that day).", example = "147") long daysUntil) {
    }
}
