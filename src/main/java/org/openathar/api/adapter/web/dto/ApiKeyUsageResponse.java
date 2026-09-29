package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Response for GET /v1/api-keys/{apiKey}/usage. */
@Schema(description = "Usage snapshot for one API key.")
public record ApiKeyUsageResponse(
        @Schema(example = "my-prayer-widget") String label,
        Instant createdAt,
        @Schema(description = "Total requests made with this key since it was issued.", example = "1423")
        long requestCount) {
}
