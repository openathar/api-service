package org.openathar.api.port.in;

import java.time.LocalDate;
import java.util.List;

import org.openathar.api.domain.IslamicEvent;

/** Use case: upcoming Islamic observances with countdowns. */
public interface EventsUseCase {

    /** The next {@code count} observances on or after {@code from}, chronologically. */
    List<IslamicEvent> upcoming(LocalDate from, int count, String locale);
}
