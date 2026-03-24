package com.dmdr.personal.portal.admin.observability.capture;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Blacklist-based sanitizer for request/response headers.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
@Component
public class DefaultHeaderMetadataSanitizer implements HeaderMetadataSanitizer {

    private static final String TRUNCATION_MARKER = "\n... [truncated]";

    private final ObjectMapper objectMapper;
    private final RequestLogObservabilityProperties properties;

    public DefaultHeaderMetadataSanitizer(ObjectMapper objectMapper, RequestLogObservabilityProperties properties) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public String sanitizeHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        try {
            Set<String> sensitiveKeys = properties.getHeaderSensitiveKeys()
                .stream()
                .filter(Objects::nonNull)
                .map(DefaultHeaderMetadataSanitizer::normalizeKey)
                .collect(Collectors.toSet());

            Map<String, List<String>> sanitized = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                List<String> values = entry.getValue();
                if (headerName == null || headerName.isBlank()) {
                    continue;
                }
                if (sensitiveKeys.contains(normalizeKey(headerName))) {
                    sanitized.put(headerName, List.of(properties.getHeaderRedactionPlaceholder()));
                } else {
                    sanitized.put(headerName, sanitizeValues(values));
                }
            }
            return truncateIfNeeded(objectMapper.writeValueAsString(sanitized));
        } catch (JsonProcessingException | RuntimeException ex) {
            return properties.getHeaderSanitizationFailedPlaceholder();
        }
    }

    private static List<String> sanitizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>(values.size());
        for (String value : values) {
            sanitized.add(value != null ? value : "");
        }
        return sanitized;
    }

    private String truncateIfNeeded(String value) {
        int maxChars = Math.max(0, properties.getHeaderMetadataMaxChars());
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= TRUNCATION_MARKER.length()) {
            return TRUNCATION_MARKER.substring(0, maxChars);
        }
        return value.substring(0, maxChars - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
    }

    private static String normalizeKey(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9]", "");
    }
}
