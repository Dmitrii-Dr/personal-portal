package com.dmdr.personal.portal.admin.observability.capture;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.classification.StackTraceTruncator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestAttributeRequestLogErrorContextTest {

    private final RequestAttributeRequestLogErrorContext context = new RequestAttributeRequestLogErrorContext(text -> text);

    @Test
    void recordApiError_shouldWriteAttributesAndCaptureContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestLogCaptureContext.attach(request, Instant.now());

        IllegalStateException error = new IllegalStateException("boom");
        context.recordApiError(request, "SOME_CODE", "Safe message", error);

        assertThat(request.getAttribute(RequestLogAttributes.ERROR_CODE)).isEqualTo("SOME_CODE");
        assertThat(request.getAttribute(RequestLogAttributes.ERROR_MESSAGE)).isEqualTo("Safe message");
        assertThat(request.getAttribute(RequestLogAttributes.STACK_TRACE).toString())
            .contains("IllegalStateException");

        RequestLogCaptureContext captureContext = RequestLogCaptureContext.current(request).orElseThrow();
        assertThat(captureContext.getErrorCode()).isEqualTo("SOME_CODE");
        assertThat(captureContext.getErrorMessage()).isEqualTo("Safe message");
        assertThat(captureContext.getStackTrace()).contains("IllegalStateException");
    }

    @Test
    void recordApiError_shouldTruncateLargeStackTraceAndNormalizeBlanks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Throwable hugeError = new RuntimeException("x".repeat(StackTraceTruncator.MAX_BYTES + 1024));

        context.recordApiError(request, " ", " ", hugeError);

        assertThat(request.getAttribute(RequestLogAttributes.ERROR_CODE)).isNull();
        assertThat(request.getAttribute(RequestLogAttributes.ERROR_MESSAGE)).isNull();

        String storedStackTrace = request.getAttribute(RequestLogAttributes.STACK_TRACE).toString();
        assertThat(storedStackTrace).endsWith(StackTraceTruncator.TRUNCATION_MARKER);
        assertThat(storedStackTrace.getBytes(StandardCharsets.UTF_8).length)
            .isLessThanOrEqualTo(StackTraceTruncator.MAX_BYTES);
    }
}
