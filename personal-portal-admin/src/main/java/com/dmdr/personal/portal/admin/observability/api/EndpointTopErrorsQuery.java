package com.dmdr.personal.portal.admin.observability.api;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

/**
 * Query for top endpoints by errors in period.
 * See docs/observability/admin-observability-public-api.md.
 */
public record EndpointTopErrorsQuery(
    LocalDate from,
    LocalDate to,
    Pageable pageable
) {
}
