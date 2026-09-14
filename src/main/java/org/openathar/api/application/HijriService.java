package org.openathar.api.application;

import java.time.LocalDate;

import org.openathar.api.domain.HijriDate;
import org.openathar.api.port.in.HijriUseCase;
import org.springframework.stereotype.Service;

/** Delegates Hijri conversion to athan-core-java. */
@Service
public class HijriService implements HijriUseCase {

    @Override
    public HijriDate convert(LocalDate date, String locale) {
        org.openathar.core.HijriDate h = org.openathar.core.Hijri.gregorianToHijri(date);
        return new HijriDate(date, h.day(), h.month(), h.year(),
            org.openathar.core.Hijri.monthName(h.month(), locale));
    }
}