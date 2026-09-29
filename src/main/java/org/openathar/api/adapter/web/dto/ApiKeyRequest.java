package org.openathar.api.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request body for POST /v1/api-keys. */
@Schema(description = "Request to issue a new self-serve API key.")
public record ApiKeyRequest(
        @Schema(description = "Optional label to help you recognize this key later (e.g. app or project name).",
            example = "my-prayer-widget")
        String label) {
}
