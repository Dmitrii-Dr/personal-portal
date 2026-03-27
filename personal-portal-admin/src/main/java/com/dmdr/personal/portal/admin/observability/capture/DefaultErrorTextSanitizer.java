package com.dmdr.personal.portal.admin.observability.capture;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Best-effort sanitizer for exception messages and stack traces stored in observability logs.
 * Blacklist-based redaction is used because stack traces are unstructured text.
 */
@Component
public class DefaultErrorTextSanitizer implements ErrorTextSanitizer {

    private static final String TRUNCATION_MARKER = "\n... [truncated]";

    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+([A-Za-z0-9._\\-+/=]+)");

    private final RequestLogObservabilityProperties properties;

    public DefaultErrorTextSanitizer(RequestLogObservabilityProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            if (!properties.isErrorTextEnabled()) {
                return truncateIfNeeded(text);
            }
            String redaction = properties.getErrorTextRedactionPlaceholder();
            String out = text;

            out = EMAIL.matcher(out).replaceAll(redaction);
            out = JWT.matcher(out).replaceAll(redaction);
            out = redactBearer(out, redaction);
            out = redactKeyValueSecrets(out, redaction);

            return truncateIfNeeded(out);
        } catch (RuntimeException ex) {
            return properties.getErrorTextSanitizationFailedPlaceholder();
        }
    }

    private String redactBearer(String input, String redaction) {
        Matcher matcher = BEARER.matcher(input);
        StringBuffer sb = new StringBuffer(input.length());
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement("Bearer " + redaction));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String redactKeyValueSecrets(String input, String redaction) {
        List<String> keys = normalizedKeys(properties.getErrorTextSensitiveKeys());
        if (keys.isEmpty()) {
            return input;
        }

        // Matches patterns like:
        // - password=...
        // - token: ...
        // - client_secret = ...
        // Keeps the key and separator, redacts the value up to a delimiter.
        String alternation = String.join("|", keys);
        Pattern keyValue = Pattern.compile(
            "(?i)\\b(" + alternation + ")\\b(\\s*[:=]\\s*)([^\\s,;\\]\\)\\}\"']+)"
        );

        Matcher matcher = keyValue.matcher(input);
        StringBuffer sb = new StringBuffer(input.length());
        while (matcher.find()) {
            String replacement = matcher.group(1) + matcher.group(2) + redaction;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String truncateIfNeeded(String value) {
        int maxChars = Math.max(0, properties.getErrorTextMaxChars());
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= TRUNCATION_MARKER.length()) {
            return TRUNCATION_MARKER.substring(0, maxChars);
        }
        return value.substring(0, maxChars - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
    }

    private static List<String> normalizedKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            normalized.add(normalizeKey(key));
        }
        return normalized;
    }

    private static String normalizeKey(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9]", "");
    }
}

