package com.dmdr.personal.portal.admin.observability.api;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Phase E query object from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public record RequestLogQuery(
    Instant from,
    Instant to,
    Integer status,
    String templatePath,
    String method,
    UUID userId,
    String errorCodeContains,
    String errorMessageContains,
    Pageable pageable
) {
}
