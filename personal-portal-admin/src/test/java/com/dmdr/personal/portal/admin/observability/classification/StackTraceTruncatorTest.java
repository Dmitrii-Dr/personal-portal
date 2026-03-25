package com.dmdr.personal.portal.admin.observability.classification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StackTraceTruncatorTest {

    @Test
    void truncate_shouldReturnNullWhenInputNull() {
        assertThat(StackTraceTruncator.truncate(null)).isNull();
    }

    @Test
    void truncate_shouldKeepStringWhenUnderMaxBytes() {
        String value = "short stacktrace";
        assertThat(StackTraceTruncator.truncate(value)).isEqualTo(value);
    }

    @Test
    void truncate_shouldKeepStringWhenExactlyMaxBytes() {
        String value = "a".repeat(StackTraceTruncator.MAX_BYTES);
        assertThat(value.getBytes(StandardCharsets.UTF_8)).hasSize(StackTraceTruncator.MAX_BYTES);
        assertThat(StackTraceTruncator.truncate(value)).isEqualTo(value);
    }

    @Test
    void truncate_shouldAppendMarkerWhenOverMaxBytes() {
        String value = "a".repeat(StackTraceTruncator.MAX_BYTES + 1);
        String truncated = StackTraceTruncator.truncate(value);

        assertThat(truncated).endsWith(StackTraceTruncator.TRUNCATION_MARKER);
        assertThat(truncated).isNotEqualTo(value);
        assertThat(truncated.getBytes(StandardCharsets.UTF_8).length)
            .isLessThanOrEqualTo(StackTraceTruncator.MAX_BYTES);
    }
}
