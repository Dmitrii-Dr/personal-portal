package com.dmdr.personal.portal.admin.observability.persistence;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.dmdr.personal.portal.admin.observability.capture.HeaderMetadataSanitizer;
import com.dmdr.personal.portal.admin.observability.auth.RequestLogUserIdResolver;
import com.dmdr.personal.portal.admin.observability.capture.RequestBodySanitizer;
import com.dmdr.personal.portal.admin.observability.capture.RequestLogCaptureContext;
import com.dmdr.personal.portal.admin.observability.routing.TemplatePathResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Default write-model factory for persisted request logs.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C4).
 */
@Component
public class DefaultRequestLogRecordFactory implements RequestLogRecordFactory {

    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_PROBLEM_JSON = "application/problem+json";

    private final RequestLogUserIdResolver userIdResolver;
    private final Clock clock;
    private final RequestBodySanitizer requestBodySanitizer;
    private final HeaderMetadataSanitizer headerMetadataSanitizer;
    private final RequestLogObservabilityProperties properties;

    public DefaultRequestLogRecordFactory(
        RequestLogUserIdResolver userIdResolver,
        Clock clock,
        RequestBodySanitizer requestBodySanitizer,
        HeaderMetadataSanitizer headerMetadataSanitizer,
        RequestLogObservabilityProperties properties
    ) {
        this.userIdResolver = Objects.requireNonNull(userIdResolver, "userIdResolver must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.requestBodySanitizer = Objects.requireNonNull(requestBodySanitizer, "requestBodySanitizer must not be null");
        this.headerMetadataSanitizer = Objects.requireNonNull(headerMetadataSanitizer, "headerMetadataSanitizer must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public RequestLogRecord build(HttpServletRequest request, HttpServletResponse response, RequestLogCaptureContext context) {
        String path = request.getRequestURI();
        String templatePath = resolveTemplatePath(request, path);
        int status = response.getStatus();
        Instant createdAt = Instant.now(clock);
        long durationMs = Math.max(0L, Duration.between(context.getStartedAt(), createdAt).toMillis());

        return new RequestLogRecord(
            path,
            templatePath,
            request.getMethod(),
            status,
            durationMs,
            userIdResolver.resolveUserId(request),
            createdAt,
            context.getErrorCode(),
            context.getErrorMessage(),
            resolveSanitizedRequestBody(request),
            resolveSanitizedRequestHeaders(request),
            resolveSanitizedResponseHeaders(response),
            context.getStackTrace()
        );
    }

    private String resolveSanitizedRequestBody(HttpServletRequest request) {
        if (!properties.isRequestBodyEnabled()) {
            return null;
        }
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        byte[] bodyBytes = wrapper.getContentAsByteArray();
        if (bodyBytes.length == 0) {
            return null;
        }
        String contentType = request.getContentType();
        if (!isJsonContentType(contentType)) {
            return properties.getRequestBodyNonJsonPlaceholder();
        }
        Charset charset = StandardCharsets.UTF_8;
        if (wrapper.getCharacterEncoding() != null) {
            try {
                charset = Charset.forName(wrapper.getCharacterEncoding());
            } catch (RuntimeException ignored) {
                charset = StandardCharsets.UTF_8;
            }
        }
        String rawBody = new String(bodyBytes, charset);
        return requestBodySanitizer.sanitizeJsonBody(rawBody);
    }

    private String resolveSanitizedRequestHeaders(HttpServletRequest request) {
        if (!properties.isRequestHeaderMetadataEnabled()) {
            return null;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(headerName);
            List<String> valueList = new ArrayList<>();
            while (values != null && values.hasMoreElements()) {
                valueList.add(values.nextElement());
            }
            headers.put(headerName, valueList);
        }
        return headerMetadataSanitizer.sanitizeHeaders(headers);
    }

    private String resolveSanitizedResponseHeaders(HttpServletResponse response) {
        if (!properties.isResponseHeaderMetadataEnabled()) {
            return null;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String headerName : response.getHeaderNames()) {
            headers.put(headerName, new ArrayList<>(response.getHeaders(headerName)));
        }
        return headerMetadataSanitizer.sanitizeHeaders(headers);
    }

    private static boolean isJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase();
        return normalized.startsWith(APPLICATION_JSON) || normalized.startsWith(APPLICATION_PROBLEM_JSON) || normalized.contains("+json");
    }

    private static String resolveTemplatePath(HttpServletRequest request, String fallbackPath) {
        Object value = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (value instanceof String template && !template.isBlank()) {
            return template;
        }
        return fallbackPath != null ? fallbackPath : TemplatePathResolver.UNKNOWN_TEMPLATE_PATH;
    }
}
