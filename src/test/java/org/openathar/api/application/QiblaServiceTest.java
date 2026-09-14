package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.Qibla;

class QiblaServiceTest {

    private final QiblaService service = new QiblaService();

    @Test
    void berlin() {
        Qibla q = service.calculate(52.52, 13.405);
        assertEquals(136.68, q.bearingDegrees(), 0.01);
    }

    @Test
    void newYork() {
        Qibla q = service.calculate(40.7128, -74.006);
        assertEquals(58.48, q.bearingDegrees(), 0.01);
    }
}