package org.openathar.api.port.in;

import java.time.LocalDate;
import java.util.List;

import org.openathar.api.domain.Place;

/** Use case: resolve a city name to coordinates and time zone. */
public interface PlacesUseCase {

    List<Place> search(String query, int limit, LocalDate at);
}
