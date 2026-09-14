package org.openathar.api.adapter.web;

import java.time.Duration;
import java.time.LocalDate;

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
public class HijriController {

    private final HijriUseCase useCase;
    private final HijriMapper mapper;

    public HijriController(HijriUseCase useCase, HijriMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getHijri(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "en") String locale) {
        HijriDate result = useCase.convert(date != null ? date : LocalDate.now(), locale);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(mapper.toResponse(result));
    }
}