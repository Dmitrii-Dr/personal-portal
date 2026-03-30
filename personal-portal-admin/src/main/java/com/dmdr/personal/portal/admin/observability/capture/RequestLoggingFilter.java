package com.dmdr.personal.portal.admin.observability.capture;

import com.dmdr.personal.portal.admin.observability.classification.HttpOutcomeClassifier;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogPersistenceGateway;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogRecord;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogRecordFactory;
import com.dmdr.personal.portal.admin.observability.routing.RequestLoggingPathPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.annotation.DependsOn;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Captures request timing/error context and emits request-log records after the response chain completes.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C7, C8).
 */
@Component
@DependsOn("entityManagerFactory")
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final int REQUEST_BODY_CACHE_LIMIT_BYTES = 128 * 1024;

    private final RequestLoggingPathPolicy pathPolicy;
    private final RequestLogRecordFactory requestLogRecordFactory;
    private final RequestLogPersistenceGateway persistenceGateway;
    private final Clock clock;

    public RequestLoggingFilter(
        RequestLoggingPathPolicy pathPolicy,
        RequestLogRecordFactory requestLogRecordFactory,
        RequestLogPersistenceGateway persistenceGateway,
        Clock clock
    ) {
        this.pathPolicy = Objects.requireNonNull(pathPolicy, "pathPolicy must not be null");
        this.requestLogRecordFactory = Objects.requireNonNull(requestLogRecordFactory, "requestLogRecordFactory must not be null");
        this.persistenceGateway = Objects.requireNonNull(persistenceGateway, "persistenceGateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    )
        throws ServletException, IOException {
        HttpServletRequest requestToProcess = wrapIfNeeded(request);
        String path = requestToProcess.getRequestURI();
        if (!pathPolicy.shouldCaptureAtAll(path)) {
            chain.doFilter(requestToProcess, response);
            return;
        }

        RequestLogCaptureContext.attach(requestToProcess, Instant.now(clock));
        try {
            chain.doFilter(requestToProcess, response);
        } finally {
            RequestLogCaptureContext.current(requestToProcess)
                .ifPresent(context -> finalizeAndEnqueue(path, requestToProcess, response, context));
        }
    }

    private static HttpServletRequest wrapIfNeeded(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return request;
        }
        if (!isMutatingMethod(request.getMethod())) {
            return request;
        }
        return new ContentCachingRequestWrapper(request, REQUEST_BODY_CACHE_LIMIT_BYTES);
    }

    private static boolean isMutatingMethod(String method) {
        if (method == null) {
            return false;
        }
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private void finalizeAndEnqueue(
        String path,
        HttpServletRequest request,
        HttpServletResponse response,
        RequestLogCaptureContext context
    ) {
        mergeErrorAttributes(request, context);

        int status = response.getStatus();
        if (pathPolicy.shouldSkipSuccess(path) && HttpOutcomeClassifier.isSuccess(status)) {
            return;
        }
        if (pathPolicy.isProbablyStaticAsset(path, null)) {
            return;
        }

        RequestLogRecord record = requestLogRecordFactory.build(request, response, context);
        persistenceGateway.enqueue(record);
    }

    private static void mergeErrorAttributes(HttpServletRequest request, RequestLogCaptureContext context) {
        context.setErrorFields(
            asNullableString(request.getAttribute(RequestLogAttributes.ERROR_CODE)),
            asNullableString(request.getAttribute(RequestLogAttributes.ERROR_MESSAGE)),
            asNullableString(request.getAttribute(RequestLogAttributes.STACK_TRACE))
        );
    }

    private static String asNullableString(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text;
    }
}
