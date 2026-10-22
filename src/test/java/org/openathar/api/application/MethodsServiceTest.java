package org.openathar.api.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openathar.api.domain.CalculationMethod;
import org.openathar.core.Method;

class MethodsServiceTest {

    private final List<CalculationMethod> methods = new MethodsService().list();

    @Test
    void listsEveryEngineMethodInOrder() {
        assertEquals(Method.values().length, methods.size());
        assertEquals("MWL", methods.get(0).id());
        assertEquals("SINGAPORE", methods.get(methods.size() - 1).id());
    }

    @Test
    void mwlAndMakkahParameters() {
        CalculationMethod mwl = methods.stream().filter(m -> m.id().equals("MWL")).findFirst().orElseThrow();
        assertEquals(18.0, mwl.fajrAngle());
        assertEquals("angle", mwl.isha().type());
        assertEquals(17.0, mwl.isha().value());
        assertEquals("minutes", mwl.maghrib().type());
        assertEquals(1.0, mwl.maghrib().value());
        assertEquals("STANDARD", mwl.midnight());
        CalculationMethod makkah = methods.stream().filter(m -> m.id().equals("MAKKAH")).findFirst().orElseThrow();
        assertEquals("minutes", makkah.isha().type());
        assertEquals(90.0, makkah.isha().value());
    }

    @Test
    void jafariUsesJafariMidnightAndEveryMethodHasAName() {
        assertEquals("JAFARI", methods.stream().filter(m -> m.id().equals("JAFARI")).findFirst().orElseThrow().midnight());
        methods.forEach(m -> assertEquals(false, m.name().equals(m.id()), m.id() + " has no human-readable name"));
    }
}
