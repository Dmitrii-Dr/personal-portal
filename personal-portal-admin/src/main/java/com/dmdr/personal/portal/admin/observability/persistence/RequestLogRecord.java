package com.dmdr.personal.portal.admin.observability.persistence;

import java.time.Instant;
import java.util.UUID;

/**
 * Request log write model detached from JPA entities.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C4).
 */
public record RequestLogRecord(
    String path,
    String templatePath,
    String method,
    int status,
    long durationMs,
    UUID userId,
    Instant createdAt,
    String errorCode,
    String errorMessage,
    String requestBody,
    String requestHeaders,
    String responseHeaders,
    String stackTrace
) {
}
