package com.dmdr.personal.portal.admin.observability.persistence;

import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Repository-backed request-log writer.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C6).
 */
@Component
public class RepositoryRequestLogWriter implements RequestLogWriter {

    private final RequestLogRepository requestLogRepository;
    private final Clock clock;

    public RepositoryRequestLogWriter(RequestLogRepository requestLogRepository, Clock clock) {
        this.requestLogRepository = Objects.requireNonNull(requestLogRepository, "requestLogRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void persistBatch(List<RequestLogRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<RequestLogEntity> entities = records.stream().map(this::toEntity).collect(Collectors.toList());
        requestLogRepository.saveAll(Objects.requireNonNull(entities, "entities must not be null"));
    }

    private RequestLogEntity toEntity(RequestLogRecord record) {
        RequestLogEntity entity = new RequestLogEntity();
        entity.setPath(record.path());
        entity.setTemplatePath(record.templatePath());
        entity.setMethod(record.method());
        entity.setStatus(record.status());
        entity.setDurationMs(record.durationMs());
        entity.setUserId(record.userId());
        entity.setCreatedAt(Instant.now(clock));
        entity.setErrorCode(record.errorCode());
        entity.setErrorMessage(record.errorMessage());
        entity.setRequestBody(record.requestBody());
        entity.setRequestHeaders(record.requestHeaders());
        entity.setResponseHeaders(record.responseHeaders());
        entity.setStackTrace(record.stackTrace());
        return entity;
    }
}
