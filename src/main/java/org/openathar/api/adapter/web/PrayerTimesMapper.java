package org.openathar.api.adapter.web;

import org.mapstruct.Mapper;
import org.openathar.api.adapter.web.dto.PrayerTimesResponse;
import org.openathar.api.domain.PrayerTimes;

@Mapper(componentModel = "spring")
public interface PrayerTimesMapper {

    PrayerTimesResponse toResponse(PrayerTimes times);
}