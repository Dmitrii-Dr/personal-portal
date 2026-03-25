package com.dmdr.personal.portal.admin.observability.capture;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DefaultRequestBodySanitizerTest {

    private final RequestLogObservabilityProperties properties = new RequestLogObservabilityProperties();
    private final DefaultRequestBodySanitizer sanitizer = new DefaultRequestBodySanitizer(new ObjectMapper(), properties);

    @Test
    void sanitizeJsonBody_shouldRedactNestedSensitiveKeys() {
        String sanitized = sanitizer.sanitizeJsonBody(
            "{\"email\":\"john@example.com\",\"profile\":{\"phone\":\"123\",\"display\":\"ok\"},\"items\":[{\"password\":\"a\"}]}"
        );

        assertThat(sanitized).contains("\"email\":\"***\"");
        assertThat(sanitized).contains("\"phone\":\"***\"");
        assertThat(sanitized).contains("\"password\":\"***\"");
        assertThat(sanitized).contains("\"display\":\"ok\"");
    }

    @Test
    void sanitizeJsonBody_shouldHandleCaseInsensitiveKeys() {
        String sanitized = sanitizer.sanitizeJsonBody("{\"Email\":\"john@example.com\",\"ACCESS_TOKEN\":\"x\"}");

        assertThat(sanitized).contains("\"Email\":\"***\"");
        assertThat(sanitized).contains("\"ACCESS_TOKEN\":\"***\"");
    }

    @Test
    void sanitizeJsonBody_shouldReturnConfiguredFailurePlaceholderForInvalidJson() {
        properties.setRequestBodySanitizationFailedPlaceholder("[failed]");

        String sanitized = sanitizer.sanitizeJsonBody("not-json");

        assertThat(sanitized).isEqualTo("[failed]");
    }

    @Test
    void sanitizeJsonBody_shouldTruncateSanitizedPayload() {
        properties.setRequestBodyMaxChars(20);

        String sanitized = sanitizer.sanitizeJsonBody("{\"name\":\"abcdefghijklmnopqrstuvwxyz\"}");

        assertThat(sanitized).hasSize(20);
        assertThat(sanitized).contains("[truncated");
    }
}
