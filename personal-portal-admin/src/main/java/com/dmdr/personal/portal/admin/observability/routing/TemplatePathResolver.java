package com.dmdr.personal.portal.admin.observability.routing;

import jakarta.servlet.http.HttpServletRequest;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B4).
 */
public interface TemplatePathResolver {

    String UNKNOWN_TEMPLATE_PATH = "UNKNOWN";

    String resolveTemplatePath(HttpServletRequest request);
}
