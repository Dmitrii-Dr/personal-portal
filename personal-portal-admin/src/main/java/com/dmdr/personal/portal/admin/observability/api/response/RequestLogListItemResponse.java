package com.dmdr.personal.portal.admin.observability.api.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Phase E list DTO from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public record RequestLogListItemResponse(
    long id,
    String path,
    String templatePath,
    String method,
    int status,
    long durationMs,
    UUID userId,
    Instant createdAt,
    String errorCode,
    String errorMessage
) {
}
