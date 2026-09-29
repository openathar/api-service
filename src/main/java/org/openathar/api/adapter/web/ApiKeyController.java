package org.openathar.api.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.openathar.api.adapter.web.dto.ApiKeyRequest;
import org.openathar.api.adapter.web.dto.ApiKeyResponse;
import org.openathar.api.adapter.web.dto.ApiKeyUsageResponse;
import org.openathar.api.adapter.web.dto.ErrorResponse;
import org.openathar.api.port.in.ApiKeyUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api-keys")
@Tag(name = "API Keys", description = "Free, instant, self-serve API keys for a higher rate limit — no email, no account.")
public class ApiKeyController {

    private final ApiKeyUseCase useCase;
    private final ApiKeyMapper mapper;

    public ApiKeyController(ApiKeyUseCase useCase, ApiKeyMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Issue a new API key",
        description = """
            Issues a free API key instantly — no email or account required. \
            Send it back as the `X-API-Key` header on `/v1/*` requests for a \
            higher rate limit than anonymous requests get. **The key is \
            shown only in this response** — Athar does not store anything \
            that lets you retrieve it again, only its usage count (see \
            `GET /v1/api-keys/{apiKey}/usage`). Treat it like a password.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Key issued",
            content = @Content(schema = @Schema(implementation = ApiKeyResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiKeyResponse> issue(@RequestBody(required = false) ApiKeyRequest request) {
        String label = request != null ? request.label() : null;
        return ResponseEntity.ok(mapper.toResponse(useCase.issue(label)));
    }

    @Operation(
        summary = "Look up usage for an API key",
        description = "Returns the label, creation date and total request count for a key you already hold.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usage found",
            content = @Content(schema = @Schema(implementation = ApiKeyUsageResponse.class))),
        @ApiResponse(responseCode = "404", description = "No such API key",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{apiKey}/usage")
    public ResponseEntity<?> usage(
            @Parameter(description = "The API key to look up.") @PathVariable String apiKey) {
        return useCase.usage(apiKey)
            .<ResponseEntity<?>>map(u -> ResponseEntity.ok(mapper.toUsageResponse(u)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ErrorResponse("unknown API key")));
    }
}
