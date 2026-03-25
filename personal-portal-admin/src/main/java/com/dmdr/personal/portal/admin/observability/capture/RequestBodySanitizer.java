package com.dmdr.personal.portal.admin.observability.capture;

/**
 * Sanitizes request bodies for persisted observability logs.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public interface RequestBodySanitizer {

    String sanitizeJsonBody(String rawBody);
}
