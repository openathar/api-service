package org.openathar.api.domain;

/** Qibla direction for a location: bearing from true north in degrees. */
public record Qibla(double latitude, double longitude, double bearingDegrees) {
}