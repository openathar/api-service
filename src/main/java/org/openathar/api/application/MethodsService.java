package org.openathar.api.application;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openathar.api.domain.CalculationMethod;
import org.openathar.api.domain.CalculationMethod.Threshold;
import org.openathar.api.port.in.MethodsUseCase;
import org.openathar.core.Method;
import org.openathar.core.Twilight;
import org.springframework.stereotype.Service;

/** Describes the engine's calculation methods from the athan-core enum, so the list can never drift from what is calculated. */
@Service
public class MethodsService implements MethodsUseCase {

    /** Human-readable authority per method id (athan-core only knows the ids). */
    private static final Map<String, String> NAMES = Map.ofEntries(
        Map.entry("MWL", "Muslim World League"),
        Map.entry("ISNA", "Islamic Society of North America"),
        Map.entry("EGYPT", "Egyptian General Authority of Survey"),
        Map.entry("MAKKAH", "Umm al-Qura University, Makkah"),
        Map.entry("KARACHI", "University of Islamic Sciences, Karachi"),
        Map.entry("TEHRAN", "Institute of Geophysics, University of Tehran"),
        Map.entry("JAFARI", "Shia Ithna-Ashari (Jafari)"),
        Map.entry("FRANCE", "France (12° / 12°)"),
        Map.entry("RUSSIA", "Spiritual Administration of Muslims of Russia"),
        Map.entry("MALAYSIA", "Department of Islamic Development Malaysia (JAKIM)"),
        Map.entry("SINGAPORE", "Majlis Ugama Islam Singapura"));

    @Override
    public List<CalculationMethod> list() {
        return Arrays.stream(Method.values())
            .map(m -> new CalculationMethod(m.name(), NAMES.getOrDefault(m.name(), m.name()), m.fajr,
                threshold(m.isha), threshold(m.maghrib), m.midnight.name()))
            .toList();
    }

    private static Threshold threshold(Twilight t) {
        return switch (t) {
            case Twilight.Angle a -> new Threshold("angle", a.degrees());
            case Twilight.Minutes m -> new Threshold("minutes", m.minutes());
        };
    }
}
