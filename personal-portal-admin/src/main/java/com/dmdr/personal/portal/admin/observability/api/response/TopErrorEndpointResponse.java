package com.dmdr.personal.portal.admin.observability.api.response;

/**
 * Top endpoint-by-errors response item.
 * See docs/observability/admin-observability-public-api.md.
 */
public record TopErrorEndpointResponse(
    String method,
    String templatePath,
    long totalCount,
    long successCount,
    long authErrorCount,
    long clientErrorCount,
    long serverErrorCount,
    long otherNonSuccessCount,
    long totalErrorCount
) {
}
