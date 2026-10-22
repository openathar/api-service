package org.openathar.api.port.in;

import java.time.Instant;

import org.openathar.api.domain.MoonInfo;

/** Use case: phase, illumination and the next new and full moon. */
public interface MoonUseCase {

    MoonInfo at(Instant at);
}
