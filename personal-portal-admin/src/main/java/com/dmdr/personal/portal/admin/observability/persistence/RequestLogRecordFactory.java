package com.dmdr.personal.portal.admin.observability.persistence;

import com.dmdr.personal.portal.admin.observability.capture.RequestLogCaptureContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds request-log write records from servlet request/response context.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C4).
 */
public interface RequestLogRecordFactory {

    RequestLogRecord build(HttpServletRequest request, HttpServletResponse response, RequestLogCaptureContext context);
}
