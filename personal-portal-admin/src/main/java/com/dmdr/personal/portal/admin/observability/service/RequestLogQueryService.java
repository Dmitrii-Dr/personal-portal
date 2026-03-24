package com.dmdr.personal.portal.admin.observability.service;

import com.dmdr.personal.portal.admin.observability.api.RequestLogQuery;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * Phase E read API from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public interface RequestLogQueryService {

    Page<RequestLogEntity> search(RequestLogQuery query);

    Optional<RequestLogEntity> findById(long id);
}
