package com.dmdr.personal.portal.admin.observability.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityContext-based user id resolver for request-log capture.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C3).
 */
@Component
public class SecurityContextRequestLogUserIdResolver implements RequestLogUserIdResolver {

    @Override
    public UUID resolveUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        if (principal instanceof String principalAsString) {
            UUID parsed = parseUuid(principalAsString);
            if (parsed != null) {
                return parsed;
            }
        }

        return parseUuid(authentication.getName());
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
