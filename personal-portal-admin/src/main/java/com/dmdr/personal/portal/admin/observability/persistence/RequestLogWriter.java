package com.dmdr.personal.portal.admin.observability.persistence;

import java.util.List;

/**
 * Persists request-log records into storage.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C6).
 */
public interface RequestLogWriter {

    void persistBatch(List<RequestLogRecord> records);
}
