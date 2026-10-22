package org.openathar.api.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;

import org.openathar.api.adapter.web.dto.MethodsResponse;
import org.openathar.api.adapter.web.dto.MethodsResponse.MethodResponse;
import org.openathar.api.adapter.web.dto.MethodsResponse.ThresholdResponse;
import org.openathar.api.domain.CalculationMethod;
import org.openathar.api.port.in.MethodsUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/methods")
@Tag(name = "Prayer Times", description = "Astronomical prayer-time calculation for any location, date and method.")
public class MethodsController {

    private final MethodsUseCase useCase;

    public MethodsController(MethodsUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
        summary = "List calculation methods",
        description = """
            The self-describing catalogue behind the `method` parameter: every \
            accepted id with its authority, Fajr angle, Isha and Maghrib rule and \
            midnight rule. Build a settings picker from this instead of hardcoding \
            a list. Cached for a day — new methods are only ever added.""")
    @ApiResponse(responseCode = "200", description = "The methods",
        content = @Content(schema = @Schema(implementation = MethodsResponse.class)))
    @SecurityRequirement(name = "ApiKeyAuth")
    @GetMapping
    public ResponseEntity<MethodsResponse> list() {
        List<MethodResponse> methods = useCase.list().stream().map(MethodsController::toResponse).toList();
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .body(new MethodsResponse(methods, "STANDARD", "NIGHT_MIDDLE"));
    }

    private static MethodResponse toResponse(CalculationMethod m) {
        return new MethodResponse(m.id(), m.name(), m.fajrAngle(),
            new ThresholdResponse(m.isha().type(), m.isha().value()),
            new ThresholdResponse(m.maghrib().type(), m.maghrib().value()), m.midnight());
    }
}
