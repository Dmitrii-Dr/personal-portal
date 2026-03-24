package com.dmdr.personal.portal.admin.observability.classification;

import java.nio.charset.StandardCharsets;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B2a).
 */
public final class StackTraceTruncator {

    public static final int MAX_BYTES = 65_536;
    public static final String TRUNCATION_MARKER = "\n... [truncated]";

    private StackTraceTruncator() {
    }

    /**
     * Truncates a stack trace string to {@link #MAX_BYTES} UTF-8 bytes and appends a marker when truncated.
     */
    public static String truncate(String stackTrace) {
        if (stackTrace == null) {
            return null;
        }

        byte[] bytes = stackTrace.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_BYTES) {
            return stackTrace;
        }

        byte[] markerBytes = TRUNCATION_MARKER.getBytes(StandardCharsets.UTF_8);
        int contentLimitBytes = Math.max(0, MAX_BYTES - markerBytes.length);

        StringBuilder builder = new StringBuilder(stackTrace.length());
        int usedBytes = 0;
        int offset = 0;
        while (offset < stackTrace.length()) {
            int codePoint = stackTrace.codePointAt(offset);
            int codePointBytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + codePointBytes > contentLimitBytes) {
                break;
            }
            builder.appendCodePoint(codePoint);
            usedBytes += codePointBytes;
            offset += Character.charCount(codePoint);
        }
        builder.append(TRUNCATION_MARKER);
        return builder.toString();
    }
}
