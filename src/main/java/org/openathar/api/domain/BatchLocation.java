package org.openathar.api.domain;

/** One location in a batch request; the label is echoed back so clients can match results. */
public record BatchLocation(String label, double latitude, double longitude, double utcOffsetHours) {
}
