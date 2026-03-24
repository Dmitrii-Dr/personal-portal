package com.dmdr.personal.portal.admin.observability.api.mapper;

import com.dmdr.personal.portal.admin.observability.api.response.RequestLogDetailResponse;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogListItemResponse;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;

/**
 * Phase E DTO mapping from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public interface RequestLogAdminMapper {

    RequestLogListItemResponse toListItem(RequestLogEntity entity);

    RequestLogDetailResponse toDetail(RequestLogEntity entity);
}
