package com.dmdr.personal.portal.admin.observability.maintenance;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Schedules request-log retention cleanup with UTC cron semantics.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D1).
 */
@Component
@ConditionalOnProperty(
    prefix = "observability.request-log",
    name = "retention-job-enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
public class RequestLogRetentionScheduler {

    private final RequestLogRetentionService requestLogRetentionService;

    public RequestLogRetentionScheduler(RequestLogRetentionService requestLogRetentionService) {
        this.requestLogRetentionService = Objects.requireNonNull(
            requestLogRetentionService,
            "requestLogRetentionService must not be null"
        );
    }

    @Scheduled(cron = "${observability.request-log.retention-cron:0 0 2 * * *}", zone = "UTC")
    public void purgeExpiredDetailRows() {
        log.info("Starting request-log retention cleanup run");
        requestLogRetentionService.purgeExpiredDetailRows();
        log.info("Finished request-log retention cleanup run");
    }
}
