package com.dmdr.personal.portal.admin.observability.capture;

import java.util.List;
import java.util.Map;

/**
 * Sanitizes request/response header metadata for persisted observability logs.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public interface HeaderMetadataSanitizer {

    String sanitizeHeaders(Map<String, List<String>> headers);
}
