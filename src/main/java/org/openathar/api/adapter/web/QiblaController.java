package org.openathar.api.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;

import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.adapter.web.dto.QiblaResponse;
import org.openathar.api.port.in.QiblaUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/qibla")
@Tag(name = "Qibla", description = "Qibla bearing (direction to the Kaaba) from any location.")
public class QiblaController {

    private final QiblaUseCase useCase;
    private final QiblaMapper mapper;

    public QiblaController(QiblaUseCase useCase, QiblaMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Calculate the Qibla bearing",
        description = """
            Returns the great-circle bearing from true north to the Kaaba \
            (21.4225241°N, 39.8261818°E) for the given location. Combine \
            with a device compass/magnetometer for an on-device Qibla \
            indicator.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bearing calculated successfully",
            content = @Content(schema = @Schema(implementation = QiblaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters, e.g. out-of-range latitude/longitude",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<?> getQibla(
            @Parameter(description = "Latitude in decimal degrees.", example = "52.52") @RequestParam double lat,
            @Parameter(description = "Longitude in decimal degrees.", example = "13.405") @RequestParam double lon) {
        if (lat < -90 || lat > 90) throw new IllegalArgumentException("latitude out of range: " + lat);
        if (lon < -180 || lon > 180) throw new IllegalArgumentException("longitude out of range: " + lon);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(mapper.toResponse(useCase.calculate(lat, lon)));
    }
}
