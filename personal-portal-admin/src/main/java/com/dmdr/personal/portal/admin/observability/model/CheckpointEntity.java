package com.dmdr.personal.portal.admin.observability.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D2).
 */
@Entity
@Table(name = "portal_checkpoint")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CheckpointEntity {

    @Id
    @Column(name = "job_name", nullable = false, length = 128)
    @EqualsAndHashCode.Include
    private String jobName;

    @Column(name = "last_processed_request_log_id", nullable = false)
    private long lastProcessedRequestLogId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
