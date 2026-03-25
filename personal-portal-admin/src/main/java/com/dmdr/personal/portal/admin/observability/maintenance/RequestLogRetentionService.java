package com.dmdr.personal.portal.admin.observability.maintenance;

/**
 * Retention cleanup contract for request-log detail rows.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D1).
 */
public interface RequestLogRetentionService {

    void purgeExpiredDetailRows();
}
