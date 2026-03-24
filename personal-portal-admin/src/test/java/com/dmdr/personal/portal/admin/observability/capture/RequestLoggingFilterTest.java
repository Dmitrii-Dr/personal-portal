package com.dmdr.personal.portal.admin.observability.capture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.persistence.RequestLogPersistenceGateway;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogRecord;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogRecordFactory;
import com.dmdr.personal.portal.admin.observability.routing.RequestLoggingPathPolicy;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

class RequestLoggingFilterTest {

    private final RequestLoggingPathPolicy pathPolicy = mock(RequestLoggingPathPolicy.class);
    private final RequestLogRecordFactory requestLogRecordFactory = mock(RequestLogRecordFactory.class);
    private final RequestLogPersistenceGateway persistenceGateway = mock(RequestLogPersistenceGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC);

    private final RequestLoggingFilter filter = new RequestLoggingFilter(
        pathPolicy,
        requestLogRecordFactory,
        persistenceGateway,
        clock
    );

    @Test
    void doFilterInternal_shouldBypassWhenPathIsNotCaptureEligible() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
        };
        when(pathPolicy.shouldCaptureAtAll("/actuator/health")).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(pathPolicy).shouldCaptureAtAll("/actuator/health");
        verify(requestLogRecordFactory, never()).build(any(), any(), any());
        verify(persistenceGateway, never()).enqueue(any());
    }

    @Test
    void doFilterInternal_shouldBuildAndEnqueueForEligiblePath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles/42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> response.setStatus(500);
        when(pathPolicy.shouldCaptureAtAll("/api/articles/42")).thenReturn(true);
        when(pathPolicy.shouldSkipSuccess("/api/articles/42")).thenReturn(false);
        when(pathPolicy.isProbablyStaticAsset("/api/articles/42", null)).thenReturn(false);
        RequestLogRecord expected = new RequestLogRecord(
            "/api/articles/42",
            "/api/articles/{id}",
            "GET",
            500,
            10L,
            UUID.randomUUID(),
            Instant.parse("2026-03-23T12:00:01Z"),
            "ERR-42",
            "boom",
            null,
            null,
            null,
            "trace"
        );
        when(requestLogRecordFactory.build(any(), any(), any())).thenReturn(expected);

        request.setAttribute(Objects.requireNonNull(RequestLogAttributes.ERROR_CODE), "ERR-42");
        request.setAttribute(Objects.requireNonNull(RequestLogAttributes.ERROR_MESSAGE), "boom");
        request.setAttribute(Objects.requireNonNull(RequestLogAttributes.STACK_TRACE), "trace");
        filter.doFilter(request, response, chain);

        verify(requestLogRecordFactory).build(any(), any(), any());
        verify(persistenceGateway).enqueue(expected);
    }

    @Test
    void doFilterInternal_shouldSkipAdminSuccessResponses() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> response.setStatus(200);
        when(pathPolicy.shouldCaptureAtAll("/admin")).thenReturn(true);
        when(pathPolicy.shouldSkipSuccess("/admin")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(requestLogRecordFactory, never()).build(any(), any(), any());
        verify(persistenceGateway, never()).enqueue(any());
    }

    @Test
    void doFilterInternal_shouldSkipStaticAssetPaths() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assets/app.css");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> response.setStatus(404);
        when(pathPolicy.shouldCaptureAtAll("/assets/app.css")).thenReturn(true);
        when(pathPolicy.shouldSkipSuccess("/assets/app.css")).thenReturn(false);
        when(pathPolicy.isProbablyStaticAsset("/assets/app.css", null)).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(requestLogRecordFactory, never()).build(any(), any(), any());
        verify(persistenceGateway, never()).enqueue(any());
    }

    @Test
    void doFilterInternal_shouldMergeErrorAttributesIntoCaptureContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/fail");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> response.setStatus(500);
        when(pathPolicy.shouldCaptureAtAll("/api/fail")).thenReturn(true);
        when(pathPolicy.shouldSkipSuccess("/api/fail")).thenReturn(false);
        when(pathPolicy.isProbablyStaticAsset("/api/fail", null)).thenReturn(false);
        when(requestLogRecordFactory.build(any(), any(), any())).thenReturn(
            new RequestLogRecord("/api/fail", "/api/fail", "POST", 500, 1L, null, Instant.now(clock), null, null, null, null, null, null)
        );

        request.setAttribute(Objects.requireNonNull(RequestLogAttributes.ERROR_CODE), "FAIL");
        request.setAttribute(Objects.requireNonNull(RequestLogAttributes.ERROR_MESSAGE), "broken");
        request.setAttribute(Objects.requireNonNull(RequestLogAttributes.STACK_TRACE), "stack");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<RequestLogCaptureContext> contextCaptor = ArgumentCaptor.forClass(RequestLogCaptureContext.class);
        verify(requestLogRecordFactory).build(any(), any(), contextCaptor.capture());
        RequestLogCaptureContext context = contextCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(context.getErrorCode()).isEqualTo("FAIL");
        org.assertj.core.api.Assertions.assertThat(context.getErrorMessage()).isEqualTo("broken");
        org.assertj.core.api.Assertions.assertThat(context.getStackTrace()).isEqualTo("stack");
    }

    @Test
    void doFilterInternal_shouldWrapMutatingRequestForBodyCaching() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/body");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"john@example.com\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            req.getInputStream().readAllBytes();
            response.setStatus(200);
        };
        when(pathPolicy.shouldCaptureAtAll("/api/body")).thenReturn(true);
        when(pathPolicy.shouldSkipSuccess("/api/body")).thenReturn(false);
        when(pathPolicy.isProbablyStaticAsset("/api/body", null)).thenReturn(false);
        when(requestLogRecordFactory.build(any(), any(), any())).thenReturn(
            new RequestLogRecord("/api/body", "/api/body", "POST", 200, 2L, null, Instant.now(clock), null, null, null, null, null, null)
        );

        filter.doFilter(request, response, chain);

        ArgumentCaptor<jakarta.servlet.http.HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(
            jakarta.servlet.http.HttpServletRequest.class
        );
        verify(requestLogRecordFactory).build(requestCaptor.capture(), any(), any());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue()).isInstanceOf(ContentCachingRequestWrapper.class);
    }
}
