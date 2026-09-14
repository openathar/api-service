package org.openathar.api.adapter.web;

import java.time.LocalDate;

import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.port.in.PrayerTimesUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prayer-times")
public class PrayerTimesController {

    private final PrayerTimesUseCase useCase;
    private final PrayerTimesMapper mapper;

    public PrayerTimesController(PrayerTimesUseCase useCase, PrayerTimesMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getPrayerTimes(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "MWL") String method,
            @RequestParam(defaultValue = "0") double utcOffset) {
        PrayerTimesQuery query = new PrayerTimesQuery(lat, lon, date, method, utcOffset);
        return ResponseEntity.ok(mapper.toResponse(useCase.calculate(query)));
    }
}