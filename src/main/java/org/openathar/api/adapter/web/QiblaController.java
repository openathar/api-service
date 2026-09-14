package org.openathar.api.adapter.web;

import org.openathar.api.port.in.QiblaUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/qibla")
public class QiblaController {

    private final QiblaUseCase useCase;
    private final QiblaMapper mapper;

    public QiblaController(QiblaUseCase useCase, QiblaMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getQibla(@RequestParam double lat, @RequestParam double lon) {
        if (lat < -90 || lat > 90) throw new IllegalArgumentException("latitude out of range: " + lat);
        if (lon < -180 || lon > 180) throw new IllegalArgumentException("longitude out of range: " + lon);
        return ResponseEntity.ok(mapper.toResponse(useCase.calculate(lat, lon)));
    }
}