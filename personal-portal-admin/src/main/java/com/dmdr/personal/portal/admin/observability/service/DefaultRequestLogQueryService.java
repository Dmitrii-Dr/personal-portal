package com.dmdr.personal.portal.admin.observability.service;

import com.dmdr.personal.portal.admin.observability.api.RequestLogQuery;
import com.dmdr.personal.portal.admin.observability.api.RequestLogSpecifications;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * Phase E query service from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
@Service
public class DefaultRequestLogQueryService implements RequestLogQueryService {

    private final RequestLogRepository requestLogRepository;

    public DefaultRequestLogQueryService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Override
    public Page<RequestLogEntity> search(RequestLogQuery query) {
        return requestLogRepository.findAll(RequestLogSpecifications.byQuery(query), query.pageable());
    }

    @Override
    public Optional<RequestLogEntity> findById(long id) {
        return requestLogRepository.findById(id);
    }
}
