package org.openathar.api.adapter.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openathar.api.adapter.web.dto.ApiKeyResponse;
import org.openathar.api.adapter.web.dto.ApiKeyUsageResponse;
import org.openathar.api.domain.ApiKey;
import org.openathar.api.domain.ApiKeyUsage;

@Mapper(componentModel = "spring")
public interface ApiKeyMapper {

    @Mapping(source = "value", target = "apiKey")
    ApiKeyResponse toResponse(ApiKey key);

    ApiKeyUsageResponse toUsageResponse(ApiKeyUsage usage);
}
