package com.dmdr.personal.portal.admin.observability.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Resolves authenticated user id for request-log records.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C3).
 */
public interface RequestLogUserIdResolver {

    UUID resolveUserId(HttpServletRequest request);
}
