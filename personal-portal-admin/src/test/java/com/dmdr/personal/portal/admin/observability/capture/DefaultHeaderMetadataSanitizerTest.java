package com.dmdr.personal.portal.admin.observability.capture;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultHeaderMetadataSanitizerTest {

    private final RequestLogObservabilityProperties properties = new RequestLogObservabilityProperties();
    private final DefaultHeaderMetadataSanitizer sanitizer = new DefaultHeaderMetadataSanitizer(new ObjectMapper(), properties);

    @Test
    void sanitizeHeaders_shouldRedactSensitiveByCaseAndFormat() {
        String sanitized = sanitizer.sanitizeHeaders(
            Map.of(
                "Authorization",
                List.of("Bearer abc"),
                "X_API_KEY",
                List.of("secret"),
                "Content-Type",
                List.of("application/json")
            )
        );

        assertThat(sanitized).contains("\"Authorization\":[\"***\"]");
        assertThat(sanitized).contains("\"X_API_KEY\":[\"***\"]");
        assertThat(sanitized).contains("\"Content-Type\":[\"application/json\"]");
    }

    @Test
    void sanitizeHeaders_shouldKeepMultiValuesForNonSensitiveHeaders() {
        String sanitized = sanitizer.sanitizeHeaders(Map.of("Accept", List.of("application/json", "text/plain")));

        assertThat(sanitized).contains("\"Accept\":[\"application/json\",\"text/plain\"]");
    }

    @Test
    void sanitizeHeaders_shouldUseFailurePlaceholderWhenSerializationFails() {
        RequestLogObservabilityProperties custom = new RequestLogObservabilityProperties();
        custom.setHeaderSanitizationFailedPlaceholder("[failed]");
        HeaderMetadataSanitizer broken = new DefaultHeaderMetadataSanitizer(new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) {
                throw new RuntimeException("boom");
            }
        }, custom);

        String sanitized = broken.sanitizeHeaders(Map.of("Accept", List.of("application/json")));
        assertThat(sanitized).isEqualTo("[failed]");
    }
}
