package com.dmdr.personal.portal.admin.observability.capture;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Error enrichment entry point for {@code @RestControllerAdvice} handlers.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C2).
 */
public interface RequestLogErrorContext {

    void recordApiError(HttpServletRequest request, String errorCode, String message, Throwable cause);
}
