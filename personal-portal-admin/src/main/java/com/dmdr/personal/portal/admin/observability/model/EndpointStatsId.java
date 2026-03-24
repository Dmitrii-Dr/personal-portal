package com.dmdr.personal.portal.admin.observability.model;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite primary key for {@link EndpointRequestStatsDailyEntity} / {@code endpoint_stats}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EndpointStatsId implements Serializable {

    private LocalDate bucketStart;
    private String method;
    private String templatePath;
}
