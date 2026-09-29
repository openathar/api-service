package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Error payload returned for every 4xx/5xx response, so clients can rely on one shape. */
@Schema(description = "Error payload returned for 4xx/5xx responses.")
public record ErrorResponse(
        @Schema(description = "Human-readable error message.", example = "latitude out of range: 95.0")
        String error) {
}
