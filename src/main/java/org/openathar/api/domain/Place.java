package org.openathar.api.domain;

/** A city with its coordinates and IANA time zone (source: GeoNames, CC BY 4.0). */
public record Place(
        String name,
        String country,
        double latitude,
        double longitude,
        String timezone,
        long population,
        double utcOffsetHours) {
}
