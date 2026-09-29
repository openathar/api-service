package org.openathar.api.domain;

import java.time.Instant;

/**
 * A self-serve API key: an opaque token plus a human label, issued once and
 * never re-shown by the API (only its usage count can be looked up again).
 */
public record ApiKey(String value, String label, Instant createdAt) {
}
