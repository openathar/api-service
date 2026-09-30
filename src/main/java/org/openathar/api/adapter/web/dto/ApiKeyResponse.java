package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Response for POST /v1/api-keys — the only time the key value is ever returned. */
@Schema(description = "A freshly issued API key. Shown once — store it yourself, Athar cannot show it to you again.")
public record ApiKeyResponse(
        @Schema(description = "The API key. Send it as the `X-API-Key` header on /v1/* requests for a higher rate limit.",
            example = "ath_x7Kp2mQ9vLr4TnW8bYc3HdF6jZs1Ne5A")
        String apiKey,
        @Schema(example = "my-prayer-widget") String label,
        Instant createdAt) {
}
