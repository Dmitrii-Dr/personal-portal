package com.dmdr.personal.portal.admin.observability.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.dmdr.personal.portal.admin.observability.auth.RequestLogUserIdResolver;
import com.dmdr.personal.portal.admin.observability.capture.HeaderMetadataSanitizer;
import com.dmdr.personal.portal.admin.observability.capture.RequestBodySanitizer;
import com.dmdr.personal.portal.admin.observability.capture.RequestAttributeRequestLogErrorContext;
import com.dmdr.personal.portal.admin.observability.capture.RequestLogCaptureContext;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

class DefaultRequestLogRecordFactoryTest {

    private static RequestLogObservabilityProperties defaultProperties() {
        return new RequestLogObservabilityProperties();
    }

    @Test
    void build_shouldCreateRecordFromHttpContext() {
        RequestLogUserIdResolver userIdResolver = mock(RequestLogUserIdResolver.class);
        RequestBodySanitizer requestBodySanitizer = mock(RequestBodySanitizer.class);
        HeaderMetadataSanitizer headerMetadataSanitizer = mock(HeaderMetadataSanitizer.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T12:00:02Z"), ZoneOffset.UTC);
        DefaultRequestLogRecordFactory factory = new DefaultRequestLogRecordFactory(
            userIdResolver,
            clock,
            requestBodySanitizer,
            headerMetadataSanitizer,
            defaultProperties()
        );
        UUID userId = UUID.randomUUID();
        when(userIdResolver.resolveUserId(any())).thenReturn(userId);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/articles/42");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/articles/{id}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        RequestLogCaptureContext.attach(request, Instant.parse("2026-03-23T12:00:00Z"));
        RequestLogCaptureContext context = RequestLogCaptureContext.current(request).orElseThrow();
        new RequestAttributeRequestLogErrorContext(text -> text).recordApiError(request, "ERR-42", "boom", new IllegalStateException("broken"));

        RequestLogRecord record = factory.build(request, response, context);

        assertThat(record.path()).isEqualTo("/api/articles/42");
        assertThat(record.templatePath()).isEqualTo("/api/articles/{id}");
        assertThat(record.method()).isEqualTo("POST");
        assertThat(record.status()).isEqualTo(500);
        assertThat(record.durationMs()).isEqualTo(2000L);
        assertThat(record.userId()).isEqualTo(userId);
        assertThat(record.createdAt()).isEqualTo(Instant.parse("2026-03-23T12:00:02Z"));
        assertThat(record.errorCode()).isEqualTo("ERR-42");
        assertThat(record.errorMessage()).isEqualTo("boom");
        assertThat(record.requestBody()).isNull();
        assertThat(record.requestHeaders()).isNull();
        assertThat(record.responseHeaders()).isNull();
        assertThat(record.stackTrace()).contains("IllegalStateException");
    }

    @Test
    void build_shouldFallbackToRequestUriWhenTemplatePatternMissing() {
        RequestLogUserIdResolver userIdResolver = mock(RequestLogUserIdResolver.class);
        RequestBodySanitizer requestBodySanitizer = mock(RequestBodySanitizer.class);
        HeaderMetadataSanitizer headerMetadataSanitizer = mock(HeaderMetadataSanitizer.class);
        DefaultRequestLogRecordFactory factory = new DefaultRequestLogRecordFactory(
            userIdResolver,
            Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC),
            requestBodySanitizer,
            headerMetadataSanitizer,
            defaultProperties()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestLogCaptureContext.attach(request, Instant.parse("2026-03-23T12:00:00Z"));

        RequestLogRecord record = factory.build(request, response, RequestLogCaptureContext.current(request).orElseThrow());

        assertThat(record.templatePath()).isEqualTo("/health");
    }

    @Test
    void build_shouldCaptureSanitizedJsonBodyFromCachingWrapper() throws IOException {
        RequestLogUserIdResolver userIdResolver = mock(RequestLogUserIdResolver.class);
        RequestBodySanitizer requestBodySanitizer = mock(RequestBodySanitizer.class);
        HeaderMetadataSanitizer headerMetadataSanitizer = mock(HeaderMetadataSanitizer.class);
        RequestLogObservabilityProperties properties = defaultProperties();
        DefaultRequestLogRecordFactory factory = new DefaultRequestLogRecordFactory(
            userIdResolver,
            Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC),
            requestBodySanitizer,
            headerMetadataSanitizer,
            properties
        );
        when(requestBodySanitizer.sanitizeJsonBody("{\"email\":\"a@b.com\",\"name\":\"test\"}")).thenReturn(
            "{\"email\":\"***\",\"name\":\"test\"}"
        );

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/profile");
        rawRequest.setContentType("application/json");
        rawRequest.setCharacterEncoding("UTF-8");
        rawRequest.setContent("{\"email\":\"a@b.com\",\"name\":\"test\"}".getBytes());
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(rawRequest);
        wrappedRequest.getInputStream().readAllBytes();

        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestLogCaptureContext.attach(wrappedRequest, Instant.parse("2026-03-23T12:00:00Z"));

        RequestLogRecord record = factory.build(wrappedRequest, response, RequestLogCaptureContext.current(wrappedRequest).orElseThrow());

        assertThat(record.requestBody()).isEqualTo("{\"email\":\"***\",\"name\":\"test\"}");
    }

    @Test
    void build_shouldUseNonJsonPlaceholderForTextPayload() throws IOException {
        RequestLogUserIdResolver userIdResolver = mock(RequestLogUserIdResolver.class);
        RequestBodySanitizer requestBodySanitizer = mock(RequestBodySanitizer.class);
        HeaderMetadataSanitizer headerMetadataSanitizer = mock(HeaderMetadataSanitizer.class);
        RequestLogObservabilityProperties properties = defaultProperties();
        properties.setRequestBodyNonJsonPlaceholder("[non-json omitted]");
        DefaultRequestLogRecordFactory factory = new DefaultRequestLogRecordFactory(
            userIdResolver,
            Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC),
            requestBodySanitizer,
            headerMetadataSanitizer,
            properties
        );

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/profile");
        rawRequest.setContentType("text/plain");
        rawRequest.setCharacterEncoding("UTF-8");
        rawRequest.setContent("name=john".getBytes());
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(rawRequest);
        wrappedRequest.getInputStream().readAllBytes();

        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestLogCaptureContext.attach(wrappedRequest, Instant.parse("2026-03-23T12:00:00Z"));

        RequestLogRecord record = factory.build(wrappedRequest, response, RequestLogCaptureContext.current(wrappedRequest).orElseThrow());

        assertThat(record.requestBody()).isEqualTo("[non-json omitted]");
    }

    @Test
    void build_shouldCaptureSanitizedRequestAndResponseHeaders() {
        RequestLogUserIdResolver userIdResolver = mock(RequestLogUserIdResolver.class);
        RequestBodySanitizer requestBodySanitizer = mock(RequestBodySanitizer.class);
        HeaderMetadataSanitizer headerMetadataSanitizer = mock(HeaderMetadataSanitizer.class);
        RequestLogObservabilityProperties properties = defaultProperties();
        DefaultRequestLogRecordFactory factory = new DefaultRequestLogRecordFactory(
            userIdResolver,
            Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC),
            requestBodySanitizer,
            headerMetadataSanitizer,
            properties
        );
        when(headerMetadataSanitizer.sanitizeHeaders(any())).thenReturn("{\"Authorization\":[\"***\"]}", "{\"Set-Cookie\":[\"***\"]}");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("Set-Cookie", "sid=1");
        RequestLogCaptureContext.attach(request, Instant.parse("2026-03-23T12:00:00Z"));

        RequestLogRecord record = factory.build(request, response, RequestLogCaptureContext.current(request).orElseThrow());

        assertThat(record.requestHeaders()).isEqualTo("{\"Authorization\":[\"***\"]}");
        assertThat(record.responseHeaders()).isEqualTo("{\"Set-Cookie\":[\"***\"]}");
    }
}
