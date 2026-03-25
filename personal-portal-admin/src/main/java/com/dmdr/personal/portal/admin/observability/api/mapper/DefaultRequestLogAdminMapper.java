package com.dmdr.personal.portal.admin.observability.api.mapper;

import com.dmdr.personal.portal.admin.observability.api.response.RequestLogDetailResponse;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogListItemResponse;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import org.springframework.stereotype.Component;

/**
 * Phase E mapper from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
@Component
public class DefaultRequestLogAdminMapper implements RequestLogAdminMapper {

    @Override
    public RequestLogListItemResponse toListItem(RequestLogEntity entity) {
        return new RequestLogListItemResponse(
            entity.getId(),
            entity.getPath(),
            entity.getTemplatePath(),
            entity.getMethod(),
            entity.getStatus(),
            entity.getDurationMs(),
            entity.getUserId(),
            entity.getCreatedAt(),
            entity.getErrorCode(),
            entity.getErrorMessage()
        );
    }

    @Override
    public RequestLogDetailResponse toDetail(RequestLogEntity entity) {
        return new RequestLogDetailResponse(
            entity.getId(),
            entity.getPath(),
            entity.getTemplatePath(),
            entity.getMethod(),
            entity.getStatus(),
            entity.getDurationMs(),
            entity.getUserId(),
            entity.getCreatedAt(),
            entity.getErrorCode(),
            entity.getErrorMessage(),
            entity.getRequestBody(),
            entity.getRequestHeaders(),
            entity.getResponseHeaders(),
            entity.getStackTrace()
        );
    }
}
