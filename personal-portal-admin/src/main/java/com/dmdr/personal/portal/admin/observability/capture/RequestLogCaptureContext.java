package com.dmdr.personal.portal.admin.observability.capture;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import lombok.Getter;

/**
 * Request-scoped capture context attached to {@link HttpServletRequest}.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C1, C2).
 */
@Getter
public final class RequestLogCaptureContext {

    private final Instant startedAt;
    private String errorCode;
    private String errorMessage;
    private String stackTrace;

    private RequestLogCaptureContext(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public static void attach(HttpServletRequest request, Instant startedAt) {
        request.setAttribute(RequestLogAttributes.CAPTURE_CONTEXT, new RequestLogCaptureContext(startedAt));
    }

    public static Optional<RequestLogCaptureContext> current(HttpServletRequest request) {
        Object value = request.getAttribute(RequestLogAttributes.CAPTURE_CONTEXT);
        if (value instanceof RequestLogCaptureContext context) {
            return Optional.of(context);
        }
        return Optional.empty();
    }

    void setErrorFields(String errorCode, String errorMessage, String stackTrace) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
    }
}
