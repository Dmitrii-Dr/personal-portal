package com.dmdr.personal.portal.admin.observability.maintenance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BatchedRequestLogRetentionServiceTest {

    @Test
    void purgeExpiredDetailRows_shouldDeleteInBatchesUntilNoRowsRemain() {
        RequestLogRepository repository = mock(RequestLogRepository.class);
        RequestLogObservabilityProperties properties = properties(7, 30, 50, 10);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T02:00:00Z"), ZoneOffset.UTC);
        BatchedRequestLogRetentionService service = new BatchedRequestLogRetentionService(repository, properties, clock);

        Instant successCutoff = Instant.parse("2026-03-16T02:00:00Z");
        Instant failureCutoff = Instant.parse("2026-02-21T02:00:00Z");
        when(repository.deleteSuccessRowsOlderThanBatch(successCutoff, 50)).thenReturn(50, 8, 0);
        when(repository.deleteFailureRowsOlderThanBatchHighStatus(failureCutoff, 50)).thenReturn(50, 12, 0);
        when(repository.deleteFailureRowsOlderThanBatchLowStatus(failureCutoff, 38)).thenReturn(0);
        when(repository.deleteFailureRowsOlderThanBatchLowStatus(failureCutoff, 50)).thenReturn(0);

        service.purgeExpiredDetailRows();

        verify(repository, times(3)).deleteSuccessRowsOlderThanBatch(successCutoff, 50);
        verify(repository, times(3)).deleteFailureRowsOlderThanBatchHighStatus(failureCutoff, 50);
        verify(repository).deleteFailureRowsOlderThanBatchLowStatus(failureCutoff, 38);
        verify(repository).deleteFailureRowsOlderThanBatchLowStatus(failureCutoff, 50);
    }

    @Test
    void purgeExpiredDetailRows_shouldStopAfterMaxBatchesPerRun() {
        RequestLogRepository repository = mock(RequestLogRepository.class);
        RequestLogObservabilityProperties properties = properties(7, 30, 10, 2);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T02:00:00Z"), ZoneOffset.UTC);
        BatchedRequestLogRetentionService service = new BatchedRequestLogRetentionService(repository, properties, clock);

        Instant successCutoff = Instant.parse("2026-03-16T02:00:00Z");
        Instant failureCutoff = Instant.parse("2026-02-21T02:00:00Z");
        when(repository.deleteSuccessRowsOlderThanBatch(successCutoff, 10)).thenReturn(10, 10, 10);
        when(repository.deleteFailureRowsOlderThanBatchHighStatus(failureCutoff, 10)).thenReturn(10, 10, 10);

        service.purgeExpiredDetailRows();

        verify(repository, times(2)).deleteSuccessRowsOlderThanBatch(successCutoff, 10);
        verify(repository, times(2)).deleteFailureRowsOlderThanBatchHighStatus(failureCutoff, 10);
        verify(repository, never()).deleteFailureRowsOlderThanBatchLowStatus(failureCutoff, 10);
    }

    @Test
    void purgeExpiredDetailRows_shouldRejectNonPositiveBatchConfiguration() {
        RequestLogRepository repository = mock(RequestLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T02:00:00Z"), ZoneOffset.UTC);
        BatchedRequestLogRetentionService invalidBatchSizeService = new BatchedRequestLogRetentionService(
            repository,
            properties(7, 30, 0, 1),
            clock
        );

        assertThatThrownBy(invalidBatchSizeService::purgeExpiredDetailRows)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retentionDeleteBatchSize");

        BatchedRequestLogRetentionService invalidMaxBatchesService = new BatchedRequestLogRetentionService(
            repository,
            properties(7, 30, 1, 0),
            clock
        );

        assertThatThrownBy(invalidMaxBatchesService::purgeExpiredDetailRows)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("retentionMaxBatchesPerRun");
    }

    private static RequestLogObservabilityProperties properties(
        int successDays,
        int failureDays,
        int batchSize,
        int maxBatchesPerRun
    ) {
        RequestLogObservabilityProperties properties = new RequestLogObservabilityProperties();
        properties.setRetentionSuccessDays(successDays);
        properties.setRetentionFailureDays(failureDays);
        properties.setRetentionDeleteBatchSize(batchSize);
        properties.setRetentionMaxBatchesPerRun(maxBatchesPerRun);
        return properties;
    }

}
