package org.openathar.api.adapter.web;

import org.mapstruct.Mapper;
import org.openathar.api.adapter.web.dto.HijriResponse;
import org.openathar.api.domain.HijriDate;

@Mapper(componentModel = "spring")
public interface HijriMapper {

    HijriResponse toResponse(HijriDate hijri);
}