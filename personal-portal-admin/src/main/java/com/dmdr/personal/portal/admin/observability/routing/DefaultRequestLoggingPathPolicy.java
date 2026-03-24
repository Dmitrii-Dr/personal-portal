package com.dmdr.personal.portal.admin.observability.routing;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B3).
 */
@Component
public class DefaultRequestLoggingPathPolicy implements RequestLoggingPathPolicy {

    private static final List<String> NO_CAPTURE_PREFIXES = List.of("/actuator", "/actuator/");
    private static final List<String> SKIP_SUCCESS_PREFIXES = List.of("/admin", "/admin/");
    private static final List<String> STATIC_SUFFIXES = List.of(
        ".css",
        ".js",
        ".mjs",
        ".map",
        ".png",
        ".jpg",
        ".jpeg",
        ".gif",
        ".svg",
        ".ico",
        ".webp",
        ".woff",
        ".woff2",
        ".ttf",
        ".eot"
    );

    @Override
    public boolean shouldCaptureAtAll(String path) {
        String normalized = normalize(path);
        return !matchesAnyPrefix(normalized, NO_CAPTURE_PREFIXES);
    }

    @Override
    public boolean shouldSkipSuccess(String path) {
        String normalized = normalize(path);
        return matchesAnyPrefix(normalized, SKIP_SUCCESS_PREFIXES);
    }

    @Override
    public boolean isProbablyStaticAsset(String path, Object handlerOrNull) {
        if (handlerOrNull instanceof ResourceHttpRequestHandler) {
            return true;
        }

        String normalized = normalize(path).toLowerCase(Locale.ROOT);
        for (String suffix : STATIC_SUFFIXES) {
            if (suffix == null || suffix.isBlank()) {
                continue;
            }
            if (normalized.endsWith(suffix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyPrefix(String path, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isBlank()) {
                continue;
            }
            String normalizedPrefix = normalize(prefix);
            if (path.equals(normalizedPrefix) || path.startsWith(normalizedPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
