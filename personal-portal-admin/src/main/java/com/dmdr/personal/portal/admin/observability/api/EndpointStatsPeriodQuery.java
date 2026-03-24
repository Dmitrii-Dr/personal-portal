package com.dmdr.personal.portal.admin.observability.api;

import java.time.LocalDate;
import java.util.List;

/**
 * Query for grouped period endpoint stats.
 * See docs/observability/design/rest-request-observability.md.
 */
public record EndpointStatsPeriodQuery(
    LocalDate from,
    LocalDate to,
    List<String> methods,
    List<String> templatePaths
) {
}
