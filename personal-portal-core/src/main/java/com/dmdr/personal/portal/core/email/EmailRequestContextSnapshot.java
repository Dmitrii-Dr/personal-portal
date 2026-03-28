package com.dmdr.personal.portal.core.email;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Captures request metadata to correlate async email failures with the originating request.
 * See docs/observability/dev/embedded-sba-actuator-design.md.
 */
public record EmailRequestContextSnapshot(
    String path,
    String method,
    UUID userId
) {

    public static final String FALLBACK_PATH = "/internal/email";
    public static final String FALLBACK_METHOD = "ASYNC";

    public static EmailRequestContextSnapshot captureCurrent() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return new EmailRequestContextSnapshot(FALLBACK_PATH, FALLBACK_METHOD, null);
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String path = request.getRequestURI() != null ? request.getRequestURI() : FALLBACK_PATH;
        String method = request.getMethod() != null ? request.getMethod() : FALLBACK_METHOD;
        return new EmailRequestContextSnapshot(path, method, resolveUserId(request));
    }

    private static UUID resolveUserId(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return null;
        }
        return parseUuid(principal.getName());
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank() || "anonymousUser".equals(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
