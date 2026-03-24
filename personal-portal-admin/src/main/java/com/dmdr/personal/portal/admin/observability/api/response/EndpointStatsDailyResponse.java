package com.dmdr.personal.portal.admin.observability.api.response;

import java.time.LocalDate;

/**
 * Phase E optional aggregates DTO from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public record EndpointStatsDailyResponse(
    LocalDate bucketStart,
    String method,
    String templatePath,
    long totalCount,
    long successCount,
    long authErrorCount,
    long clientErrorCount,
    long serverErrorCount,
    long otherNonSuccessCount
) {
}
