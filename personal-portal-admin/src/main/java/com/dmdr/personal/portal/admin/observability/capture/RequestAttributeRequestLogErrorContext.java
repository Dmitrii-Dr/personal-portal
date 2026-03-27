package com.dmdr.personal.portal.admin.observability.capture;

import com.dmdr.personal.portal.admin.observability.classification.StackTraceTruncator;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Stores API error metadata in request attributes for later filter-side readout.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C2).
 */
@Component
public class RequestAttributeRequestLogErrorContext implements RequestLogErrorContext {

    private final ErrorTextSanitizer errorTextSanitizer;

    public RequestAttributeRequestLogErrorContext(ErrorTextSanitizer errorTextSanitizer) {
        this.errorTextSanitizer = Objects.requireNonNull(errorTextSanitizer, "errorTextSanitizer must not be null");
    }

    @Override
    public void recordApiError(HttpServletRequest request, String errorCode, String message, Throwable cause) {
        String sanitizedMessage = errorTextSanitizer.sanitize(normalize(message));
        String stackTrace = errorTextSanitizer.sanitize(truncateStackTrace(cause));

        request.setAttribute(RequestLogAttributes.ERROR_CODE, normalize(errorCode));
        request.setAttribute(RequestLogAttributes.ERROR_MESSAGE, sanitizedMessage);
        request.setAttribute(RequestLogAttributes.STACK_TRACE, stackTrace);

        RequestLogCaptureContext.current(request)
            .ifPresent(context -> context.setErrorFields(normalize(errorCode), sanitizedMessage, stackTrace));
    }

    private static String truncateStackTrace(Throwable cause) {
        if (cause == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        return StackTraceTruncator.truncate(writer.toString());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
