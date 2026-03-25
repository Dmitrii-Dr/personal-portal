package com.dmdr.personal.portal.admin.observability.api.mapper;

import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsDailyResponse;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import org.springframework.stereotype.Component;

@Component
public class DefaultEndpointStatsDailyMapper implements EndpointStatsDailyMapper {

    @Override
    public EndpointStatsDailyResponse toResponse(EndpointRequestStatsDailyEntity entity) {
        return new EndpointStatsDailyResponse(
            entity.getBucketStart(),
            entity.getMethod(),
            entity.getTemplatePath(),
            entity.getTotalCount(),
            entity.getSuccessCount(),
            entity.getAuthErrorCount(),
            entity.getClientErrorCount(),
            entity.getServerErrorCount(),
            entity.getOtherNonSuccessCount()
        );
    }
}
