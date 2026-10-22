package org.openathar.api.domain;

/**
 * A prayer-time calculation method as used by the engine. Fajr is always a
 * sun-depression angle; Isha and Maghrib are either an angle or a fixed
 * number of minutes (after sunset or after Maghrib respectively).
 */
public record CalculationMethod(
        String id,
        String name,
        double fajrAngle,
        Threshold isha,
        Threshold maghrib,
        String midnight) {

    /** {@code type} is {@code "angle"} (degrees below the horizon) or {@code "minutes"}. */
    public record Threshold(String type, double value) {
    }
}
