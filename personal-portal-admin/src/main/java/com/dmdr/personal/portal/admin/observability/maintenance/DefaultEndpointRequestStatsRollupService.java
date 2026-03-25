package com.dmdr.personal.portal.admin.observability.maintenance;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.dmdr.personal.portal.admin.observability.classification.HttpOutcomeBucket;
import com.dmdr.personal.portal.admin.observability.classification.HttpOutcomeClassifier;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import com.dmdr.personal.portal.admin.observability.model.CheckpointEntity;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import com.dmdr.personal.portal.admin.observability.repository.EndpointRequestStatsDailyRepository;
import com.dmdr.personal.portal.admin.observability.repository.ObservabilityRollupCheckpointRepository;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase D / Step D2).
 */
@Service
@Slf4j
public class DefaultEndpointRequestStatsRollupService implements EndpointRequestStatsRollupService {

    static final String ROLLUP_JOB_NAME = "endpoint_stats_rollup";

    private final RequestLogRepository requestLogRepository;
    private final EndpointRequestStatsDailyRepository endpointRequestStatsDailyRepository;
    private final ObservabilityRollupCheckpointRepository checkpointRepository;
    private final RequestLogObservabilityProperties properties;
    private final Clock clock;

    public DefaultEndpointRequestStatsRollupService(
        RequestLogRepository requestLogRepository,
        EndpointRequestStatsDailyRepository endpointRequestStatsDailyRepository,
        ObservabilityRollupCheckpointRepository checkpointRepository,
        RequestLogObservabilityProperties properties,
        Clock clock
    ) {
        this.requestLogRepository = Objects.requireNonNull(requestLogRepository, "requestLogRepository must not be null");
        this.endpointRequestStatsDailyRepository = Objects.requireNonNull(
            endpointRequestStatsDailyRepository,
            "endpointRequestStatsDailyRepository must not be null"
        );
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository, "checkpointRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void rollUpSinceLastCheckpoint() {
        int batchSize = positive(properties.getRollupBatchSize(), "rollupBatchSize");
        Instant now = Instant.now(clock);
        LocalDate todayUtc = now.atZone(ZoneOffset.UTC).toLocalDate();

        CheckpointEntity checkpoint = checkpointRepository.findById(ROLLUP_JOB_NAME).orElse(null);
        boolean checkpointCreated = checkpoint == null;
        if (checkpoint == null) {
            checkpoint = newCheckpoint(now);
        }

        long startIdExclusive = checkpoint.getLastProcessedRequestLogId();
        List<RequestLogEntity> logs = requestLogRepository.findByIdGreaterThanOrderByIdAsc(
            startIdExclusive,
            PageRequest.of(0, batchSize)
        );

        if (logs.isEmpty()) {
            ensureTodayZeroRows(todayUtc);
            if (checkpointCreated) {
                checkpointRepository.save(checkpoint);
            }
            log.info("Rollup found no new request logs (checkpoint={})", startIdExclusive);
            return;
        }

        Map<AggregateKey, AggregateDelta> deltas = new LinkedHashMap<>();
        long maxProcessedId = startIdExclusive;
        for (RequestLogEntity log : logs) {
            AggregateKey key = new AggregateKey(
                log.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                log.getMethod(),
                log.getTemplatePath()
            );
            deltas.computeIfAbsent(key, ignored -> new AggregateDelta()).increment(log.getStatus());
            if (log.getId() != null) {
                maxProcessedId = Math.max(maxProcessedId, log.getId());
            }
        }

        for (Map.Entry<AggregateKey, AggregateDelta> entry : deltas.entrySet()) {
            upsertAggregate(entry.getKey(), entry.getValue());
        }
        ensureTodayZeroRows(todayUtc);

        checkpoint.setLastProcessedRequestLogId(maxProcessedId);
        checkpoint.setUpdatedAt(now);
        checkpointRepository.save(checkpoint);
        log.info(
            "Rollup processed request logs: count={}, fromExclusiveId={}, toInclusiveId={}",
            logs.size(),
            startIdExclusive,
            maxProcessedId
        );
    }

    @SuppressWarnings("null")
    private void ensureTodayZeroRows(LocalDate todayUtc) {
        for (EndpointRequestStatsDailyRepository.MethodTemplatePairProjection pair : endpointRequestStatsDailyRepository.findDistinctMethodTemplatePairs()) {
            AggregateKey key = new AggregateKey(todayUtc, pair.getMethod(), pair.getTemplatePath());
            boolean exists = endpointRequestStatsDailyRepository.findByBucketStartAndMethodAndTemplatePath(
                key.bucketStart(),
                key.method(),
                key.templatePath()
            ).isPresent();
            if (!exists) {
                Objects.requireNonNull(
                    endpointRequestStatsDailyRepository.save(newStats(key)),
                    "saved stats must not be null"
                );
            }
        }
    }

    private void upsertAggregate(AggregateKey key, AggregateDelta delta) {
        EndpointRequestStatsDailyEntity stats = endpointRequestStatsDailyRepository
            .findByBucketStartAndMethodAndTemplatePath(key.bucketStart(), key.method(), key.templatePath())
            .orElseGet(() -> newStats(key));

        stats.setTotalCount(stats.getTotalCount() + delta.totalCount);
        stats.setSuccessCount(stats.getSuccessCount() + delta.successCount);
        stats.setAuthErrorCount(stats.getAuthErrorCount() + delta.authErrorCount);
        stats.setClientErrorCount(stats.getClientErrorCount() + delta.clientErrorCount);
        stats.setServerErrorCount(stats.getServerErrorCount() + delta.serverErrorCount);
        stats.setOtherNonSuccessCount(stats.getOtherNonSuccessCount() + delta.otherNonSuccessCount);

        endpointRequestStatsDailyRepository.save(stats);
    }

    private EndpointRequestStatsDailyEntity newStats(AggregateKey key) {
        EndpointRequestStatsDailyEntity stats = new EndpointRequestStatsDailyEntity();
        stats.setBucketStart(key.bucketStart());
        stats.setMethod(key.method());
        stats.setTemplatePath(key.templatePath());
        return stats;
    }

    private CheckpointEntity newCheckpoint(Instant now) {
        CheckpointEntity checkpoint = new CheckpointEntity();
        checkpoint.setJobName(ROLLUP_JOB_NAME);
        checkpoint.setLastProcessedRequestLogId(0L);
        checkpoint.setUpdatedAt(now);
        return checkpoint;
    }

    private int positive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " must be > 0");
        }
        return value;
    }

    private record AggregateKey(java.time.LocalDate bucketStart, String method, String templatePath) {
    }

    private static final class AggregateDelta {
        private long totalCount;
        private long successCount;
        private long authErrorCount;
        private long clientErrorCount;
        private long serverErrorCount;
        private long otherNonSuccessCount;

        private void increment(int status) {
            totalCount++;
            HttpOutcomeBucket bucket = HttpOutcomeClassifier.classify(status);
            switch (bucket) {
                case SUCCESS_2XX -> successCount++;
                case AUTH_ERROR_401_403 -> authErrorCount++;
                case CLIENT_ERROR_4XX_OTHER -> clientErrorCount++;
                case SERVER_ERROR_5XX -> serverErrorCount++;
                case OTHER_NON_SUCCESS -> otherNonSuccessCount++;
            }
        }
    }
}
