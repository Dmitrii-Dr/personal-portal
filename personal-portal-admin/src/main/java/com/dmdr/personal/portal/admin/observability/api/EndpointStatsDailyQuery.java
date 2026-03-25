package com.dmdr.personal.portal.admin.observability.api;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Query for read-only endpoint stats listing.
 */
public record EndpointStatsDailyQuery(
    LocalDate from,
    LocalDate to,
    List<String> methods,
    List<String> templatePaths,
    Pageable pageable
) {
}
