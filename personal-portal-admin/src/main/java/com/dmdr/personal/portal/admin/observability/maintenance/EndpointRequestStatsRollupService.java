package com.dmdr.personal.portal.admin.observability.maintenance;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D2).
 */
public interface EndpointRequestStatsRollupService {

    void rollUpSinceLastCheckpoint();
}
