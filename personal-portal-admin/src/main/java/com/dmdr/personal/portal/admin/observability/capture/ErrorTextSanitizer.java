package com.dmdr.personal.portal.admin.observability.capture;

/**
 * Sanitizes error text (messages/stack traces) for persisted observability logs.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C2).
 */
public interface ErrorTextSanitizer {

    String sanitize(String text);
}

