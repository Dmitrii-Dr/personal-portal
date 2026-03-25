package com.dmdr.personal.portal.admin.observability.persistence;

/**
 * Async gateway for best-effort request-log persistence.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C5).
 */
public interface RequestLogPersistenceGateway {

    void enqueue(RequestLogRecord record);
}
