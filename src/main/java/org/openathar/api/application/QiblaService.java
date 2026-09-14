package org.openathar.api.application;

import org.openathar.api.domain.Qibla;
import org.openathar.api.port.in.QiblaUseCase;
import org.springframework.stereotype.Service;

/** Delegates Qibla calculation to athan-core-java. */
@Service
public class QiblaService implements QiblaUseCase {

    @Override
    public Qibla calculate(double latitude, double longitude) {
        return new Qibla(latitude, longitude, org.openathar.core.Qibla.bearing(latitude, longitude));
    }
}