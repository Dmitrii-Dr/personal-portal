package com.dmdr.personal.portal.admin.observability.api.mapper;

import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsDailyResponse;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;

public interface EndpointStatsDailyMapper {

    EndpointStatsDailyResponse toResponse(EndpointRequestStatsDailyEntity entity);
}
