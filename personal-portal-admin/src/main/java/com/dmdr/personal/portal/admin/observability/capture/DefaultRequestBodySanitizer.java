package com.dmdr.personal.portal.admin.observability.capture;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Default JSON request-body sanitizer for observability persistence.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
@Component
public class DefaultRequestBodySanitizer implements RequestBodySanitizer {

    private static final String TRUNCATION_MARKER = "\n... [truncated]";

    private final ObjectMapper objectMapper;
    private final RequestLogObservabilityProperties properties;

    public DefaultRequestBodySanitizer(ObjectMapper objectMapper, RequestLogObservabilityProperties properties) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public String sanitizeJsonBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            Set<String> sensitiveKeys = normalizedKeys(properties.getRequestBodySensitiveKeys());
            redactSensitiveFields(root, sensitiveKeys);
            return truncateIfNeeded(objectMapper.writeValueAsString(root));
        } catch (JsonProcessingException ex) {
            return properties.getRequestBodySanitizationFailedPlaceholder();
        }
    }

    private void redactSensitiveFields(JsonNode node, Set<String> sensitiveKeys) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = objectNode.get(fieldName);
                if (sensitiveKeys.contains(normalizeKey(fieldName))) {
                    objectNode.put(fieldName, properties.getRequestBodyRedactionPlaceholder());
                } else {
                    redactSensitiveFields(child, sensitiveKeys);
                }
            });
            return;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode child : arrayNode) {
                redactSensitiveFields(child, sensitiveKeys);
            }
        }
    }

    private String truncateIfNeeded(String value) {
        int maxChars = Math.max(0, properties.getRequestBodyMaxChars());
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= TRUNCATION_MARKER.length()) {
            return TRUNCATION_MARKER.substring(0, maxChars);
        }
        return value.substring(0, maxChars - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
    }

    private static Set<String> normalizedKeys(List<String> keys) {
        Set<String> normalized = new HashSet<>();
        if (keys == null) {
            return normalized;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                normalized.add(normalizeKey(key));
            }
        }
        return normalized;
    }

    private static String normalizeKey(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9]", "");
    }
}
