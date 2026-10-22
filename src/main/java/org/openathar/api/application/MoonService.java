package org.openathar.api.application;

import java.time.Instant;

import org.openathar.api.domain.MoonInfo;
import org.openathar.api.port.in.MoonUseCase;
import org.springframework.stereotype.Service;

/**
 * Moon phase from the true times of new and full moon (Meeus, "Astronomical
 * Algorithms", chapter 49 — the principal periodic terms plus the planetary
 * corrections, good to a couple of minutes). Illumination and age are
 * interpolated between the surrounding true new moons.
 *
 * This is astronomy, not a religious ruling: the start of a Hijri month
 * depends on the actual crescent sighting, not on the moment of conjunction.
 */
@Service
public class MoonService implements MoonUseCase {

    private static final double SYNODIC_MONTH = 29.530588861;
    /** Julian day of the Unix epoch. */
    private static final double JD_UNIX_EPOCH = 2440587.5;
    /** TT − UT in seconds, a good value for the 2020s; the effect on the result is a minute at most. */
    private static final double DELTA_T_SECONDS = 69.0;

    @Override
    public MoonInfo at(Instant at) {
        double jd = JD_UNIX_EPOCH + at.toEpochMilli() / 86_400_000.0;
        // Lunation number of the new moon at or before 'at'.
        long k = (long) Math.floor((jd - 2451550.09766) / SYNODIC_MONTH);
        Instant prev = instant(phase(k, 0));
        while (prev.isAfter(at)) {
            k--;
            prev = instant(phase(k, 0));
        }
        Instant next = instant(phase(k + 1, 0));
        while (!next.isAfter(at)) {
            k++;
            prev = next;
            next = instant(phase(k + 1, 0));
        }
        Instant full = instant(phase(k, 0.5));
        if (!full.isAfter(at)) full = instant(phase(k + 1, 0.5));

        double span = (next.toEpochMilli() - prev.toEpochMilli()) / 86_400_000.0;
        double age = (at.toEpochMilli() - prev.toEpochMilli()) / 86_400_000.0;
        double fraction = age / span;
        double illumination = (1 - Math.cos(2 * Math.PI * fraction)) / 2;
        return new MoonInfo(at, phaseKey(fraction), illumination, age, fraction * 360, prev, next, full);
    }

    static String phaseKey(double f) {
        if (f < 0.03 || f > 0.97) return "new";
        if (f < 0.22) return "waxing-crescent";
        if (f < 0.28) return "first-quarter";
        if (f < 0.47) return "waxing-gibbous";
        if (f < 0.53) return "full";
        if (f < 0.72) return "waning-gibbous";
        if (f < 0.78) return "last-quarter";
        return "waning-crescent";
    }

    private static Instant instant(double jdUt) {
        return Instant.ofEpochMilli(Math.round((jdUt - JD_UNIX_EPOCH) * 86_400_000.0));
    }

    /**
     * Julian day (UT) of the new moon (phase 0) or full moon (phase 0.5) of
     * lunation {@code lunation}, i.e. k = lunation + phase.
     */
    static double phase(long lunation, double phase) {
        double k = lunation + phase;
        double t = k / 1236.85;
        double jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * t * t - 0.000000150 * t * t * t
            + 0.00000000073 * t * t * t * t;
        double e = 1 - 0.002516 * t - 0.0000074 * t * t;
        double m = rad(2.5534 + 29.10535670 * k - 0.0000014 * t * t - 0.00000011 * t * t * t);
        double mp = rad(201.5643 + 385.81693528 * k + 0.0107582 * t * t + 0.00001238 * t * t * t - 0.000000058 * t * t * t * t);
        double f = rad(160.7108 + 390.67050284 * k - 0.0016118 * t * t - 0.00000227 * t * t * t + 0.000000011 * t * t * t * t);
        double om = rad(124.7746 - 1.56375588 * k + 0.0020672 * t * t + 0.00000215 * t * t * t);
        double c;
        if (phase == 0) {
            c = -0.40720 * Math.sin(mp) + 0.17241 * e * Math.sin(m) + 0.01608 * Math.sin(2 * mp) + 0.01039 * Math.sin(2 * f)
                + 0.00739 * e * Math.sin(mp - m) - 0.00514 * e * Math.sin(mp + m) + 0.00208 * e * e * Math.sin(2 * m)
                - 0.00111 * Math.sin(mp - 2 * f) - 0.00057 * Math.sin(mp + 2 * f) + 0.00056 * e * Math.sin(2 * mp + m)
                - 0.00042 * Math.sin(3 * mp) + 0.00042 * e * Math.sin(m + 2 * f) + 0.00038 * e * Math.sin(m - 2 * f)
                - 0.00024 * e * Math.sin(2 * mp - m) - 0.00017 * Math.sin(om);
        } else {
            c = -0.40614 * Math.sin(mp) + 0.17302 * e * Math.sin(m) + 0.01614 * Math.sin(2 * mp) + 0.01043 * Math.sin(2 * f)
                + 0.00734 * e * Math.sin(mp - m) - 0.00515 * e * Math.sin(mp + m) + 0.00209 * e * e * Math.sin(2 * m)
                - 0.00111 * Math.sin(mp - 2 * f) - 0.00057 * Math.sin(mp + 2 * f) + 0.00056 * e * Math.sin(2 * mp + m)
                - 0.00042 * Math.sin(3 * mp) + 0.00042 * e * Math.sin(m + 2 * f) + 0.00038 * e * Math.sin(m - 2 * f)
                - 0.00024 * e * Math.sin(2 * mp - m) - 0.00017 * Math.sin(om);
        }
        double[] a = {
            299.77 + 0.107408 * k - 0.009173 * t * t, 251.88 + 0.016321 * k, 251.83 + 26.651886 * k,
            349.42 + 36.412478 * k, 84.66 + 18.206239 * k, 141.74 + 53.303771 * k, 207.14 + 2.453732 * k,
            154.84 + 7.306860 * k, 34.52 + 27.261239 * k, 207.19 + 0.121824 * k, 291.34 + 1.844379 * k,
            161.72 + 24.198154 * k, 239.56 + 25.513099 * k, 331.55 + 3.592518 * k };
        double[] w = { 0.000325, 0.000165, 0.000164, 0.000126, 0.00011, 0.000062, 0.00006, 0.000056, 0.000047,
            0.000042, 0.00004, 0.000037, 0.000035, 0.000023 };
        double add = 0;
        for (int i = 0; i < a.length; i++) add += w[i] * Math.sin(rad(a[i]));
        return jde + c + add - DELTA_T_SECONDS / 86_400.0;
    }

    private static double rad(double deg) {
        return Math.toRadians(deg);
    }
}
