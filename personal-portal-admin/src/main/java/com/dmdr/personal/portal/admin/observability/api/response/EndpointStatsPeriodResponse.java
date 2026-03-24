package com.dmdr.personal.portal.admin.observability.api.response;

/**
 * Grouped and summed period stats response.
 * See docs/observability/design/rest-request-observability.md.
 */
public record EndpointStatsPeriodResponse(
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
