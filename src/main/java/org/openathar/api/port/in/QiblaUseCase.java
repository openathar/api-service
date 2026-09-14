package org.openathar.api.port.in;

import org.openathar.api.domain.Qibla;

/** Use case: calculate the Qibla direction for a location. */
public interface QiblaUseCase {

    Qibla calculate(double latitude, double longitude);
}