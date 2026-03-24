package com.dmdr.personal.portal.admin.observability.maintenance;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batched retention cleanup implementation for request-log detail rows.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D1).
 */
@Service
@Slf4j
public class BatchedRequestLogRetentionService implements RequestLogRetentionService {

    private final RequestLogRepository requestLogRepository;
    private final RequestLogObservabilityProperties properties;
    private final Clock clock;

    public BatchedRequestLogRetentionService(
        RequestLogRepository requestLogRepository,
        RequestLogObservabilityProperties properties,
        Clock clock
    ) {
        this.requestLogRepository = Objects.requireNonNull(requestLogRepository, "requestLogRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void purgeExpiredDetailRows() {
        int batchSize = positive(properties.getRetentionDeleteBatchSize(), "retentionDeleteBatchSize");
        int maxBatchesPerRun = positive(properties.getRetentionMaxBatchesPerRun(), "retentionMaxBatchesPerRun");
        Instant now = Instant.now(clock);
        Instant successCutoff = now.minus(properties.getRetentionSuccessDays(), ChronoUnit.DAYS);
        Instant failureCutoff = now.minus(properties.getRetentionFailureDays(), ChronoUnit.DAYS);

        int successDeleted = runBatches(maxBatchesPerRun, () -> deleteSuccessBatch(successCutoff, batchSize));
        int failureDeleted = runBatches(maxBatchesPerRun, () -> deleteFailureBatch(failureCutoff, batchSize));
        log.info(
            "Request-log retention cleanup completed: successDeleted={}, failureDeleted={}, batchSize={}, maxBatchesPerRun={}",
            successDeleted,
            failureDeleted,
            batchSize,
            maxBatchesPerRun
        );
    }

    private int positive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " must be > 0");
        }
        return value;
    }

    private int runBatches(int maxBatchesPerRun, BatchDelete batchDelete) {
        int totalDeleted = 0;
        for (int i = 0; i < maxBatchesPerRun; i++) {
            int deleted = batchDelete.delete();
            if (deleted <= 0) {
                return totalDeleted;
            }
            totalDeleted += deleted;
        }
        return totalDeleted;
    }

    @Transactional
    protected int deleteSuccessBatch(Instant cutoff, int batchSize) {
        return requestLogRepository.deleteSuccessRowsOlderThanBatch(cutoff, batchSize);
    }

    @Transactional
    protected int deleteFailureBatch(Instant cutoff, int batchSize) {
        int deletedHighStatus = requestLogRepository.deleteFailureRowsOlderThanBatchHighStatus(cutoff, batchSize);
        int remaining = Math.max(batchSize - deletedHighStatus, 0);
        if (remaining == 0) {
            return deletedHighStatus;
        }
        int deletedLowStatus = requestLogRepository.deleteFailureRowsOlderThanBatchLowStatus(cutoff, remaining);
        return deletedHighStatus + deletedLowStatus;
    }

    @FunctionalInterface
    private interface BatchDelete {
        int delete();
    }
}
