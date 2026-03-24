package com.dmdr.personal.portal.admin.observability.maintenance;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Schedules endpoint daily aggregate rollups with UTC cron semantics.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D2).
 */
@Component
@ConditionalOnProperty(
    prefix = "observability.request-log",
    name = "rollup-job-enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
public class EndpointRequestStatsRollupScheduler {

    private final EndpointRequestStatsRollupService endpointRequestStatsRollupService;

    public EndpointRequestStatsRollupScheduler(EndpointRequestStatsRollupService endpointRequestStatsRollupService) {
        this.endpointRequestStatsRollupService = Objects.requireNonNull(
            endpointRequestStatsRollupService,
            "endpointRequestStatsRollupService must not be null"
        );
    }

    @Scheduled(cron = "${observability.request-log.rollup-cron:0 0 * * * *}", zone = "UTC")
    public void rollUpSinceLastCheckpoint() {
        log.info("Starting request-log endpoint stats rollup run");
        endpointRequestStatsRollupService.rollUpSinceLastCheckpoint();
        log.info("Finished request-log endpoint stats rollup run");
    }
}
