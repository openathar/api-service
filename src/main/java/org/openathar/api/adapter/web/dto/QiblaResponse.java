package org.openathar.api.adapter.web.dto;

/** REST response for GET /v1/qibla. */
public record QiblaResponse(double latitude, double longitude, double bearingDegrees) {
}