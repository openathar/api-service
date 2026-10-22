package org.openathar.api.domain;

import java.time.Instant;

/** Moon state at one instant. All instants are UTC. */
public record MoonInfo(
        Instant at,
        String phase,
        double illumination,
        double ageDays,
        double phaseAngleDegrees,
        Instant previousNewMoon,
        Instant nextNewMoon,
        Instant nextFullMoon) {
}
