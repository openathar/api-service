package org.openathar.api.domain;

import java.time.Instant;

/** Usage snapshot for an existing API key: who it belongs to and how much it has been used. */
public record ApiKeyUsage(String value, String label, Instant createdAt, long requestCount) {
}
