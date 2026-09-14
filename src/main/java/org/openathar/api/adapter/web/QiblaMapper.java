package org.openathar.api.adapter.web;

import org.mapstruct.Mapper;
import org.openathar.api.adapter.web.dto.QiblaResponse;
import org.openathar.api.domain.Qibla;

@Mapper(componentModel = "spring")
public interface QiblaMapper {

    QiblaResponse toResponse(Qibla qibla);
}