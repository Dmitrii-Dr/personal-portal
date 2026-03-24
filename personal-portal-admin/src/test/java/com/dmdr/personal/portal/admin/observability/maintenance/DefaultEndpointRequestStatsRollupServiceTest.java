package com.dmdr.personal.portal.admin.observability.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import com.dmdr.personal.portal.admin.observability.model.CheckpointEntity;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import com.dmdr.personal.portal.admin.observability.repository.EndpointRequestStatsDailyRepository;
import com.dmdr.personal.portal.admin.observability.repository.ObservabilityRollupCheckpointRepository;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

@SuppressWarnings("null")
class DefaultEndpointRequestStatsRollupServiceTest {

    @Test
    void rollUpSinceLastCheckpoint_shouldInitializeCheckpointWhenMissingAndNoRows() {
        RequestLogRepository requestLogRepository = mock(RequestLogRepository.class);
        EndpointRequestStatsDailyRepository statsRepository = mock(EndpointRequestStatsDailyRepository.class);
        ObservabilityRollupCheckpointRepository checkpointRepository = mock(ObservabilityRollupCheckpointRepository.class);
        RequestLogObservabilityProperties properties = properties(100);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC);

        when(checkpointRepository.findById(DefaultEndpointRequestStatsRollupService.ROLLUP_JOB_NAME)).thenReturn(Optional.empty());
        when(requestLogRepository.findByIdGreaterThanOrderByIdAsc(eq(0L), any(Pageable.class))).thenReturn(List.of());
        when(statsRepository.findDistinctMethodTemplatePairs()).thenReturn(Collections.emptyList());
        when(statsRepository.save(any(EndpointRequestStatsDailyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultEndpointRequestStatsRollupService service = new DefaultEndpointRequestStatsRollupService(
            requestLogRepository,
            statsRepository,
            checkpointRepository,
            properties,
            clock
        );

        service.rollUpSinceLastCheckpoint();

        ArgumentCaptor<CheckpointEntity> captor = ArgumentCaptor.forClass(
            CheckpointEntity.class
        );
        verify(checkpointRepository).save(captor.capture());
        CheckpointEntity checkpoint = Objects.requireNonNull(captor.getValue());
        assertThat(checkpoint.getJobName()).isEqualTo(DefaultEndpointRequestStatsRollupService.ROLLUP_JOB_NAME);
        assertThat(checkpoint.getLastProcessedRequestLogId()).isEqualTo(0L);
        assertThat(checkpoint.getUpdatedAt()).isEqualTo(Instant.parse("2026-03-23T10:00:00Z"));
        verify(statsRepository, never()).save(any(EndpointRequestStatsDailyEntity.class));
    }

    @Test
    void rollUpSinceLastCheckpoint_shouldUpsertAggregatesAndAdvanceCheckpoint() {
        RequestLogRepository requestLogRepository = mock(RequestLogRepository.class);
        EndpointRequestStatsDailyRepository statsRepository = mock(EndpointRequestStatsDailyRepository.class);
        ObservabilityRollupCheckpointRepository checkpointRepository = mock(ObservabilityRollupCheckpointRepository.class);
        RequestLogObservabilityProperties properties = properties(100);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T11:00:00Z"), ZoneOffset.UTC);

        CheckpointEntity checkpoint = new CheckpointEntity();
        checkpoint.setJobName(DefaultEndpointRequestStatsRollupService.ROLLUP_JOB_NAME);
        checkpoint.setLastProcessedRequestLogId(10L);
        checkpoint.setUpdatedAt(Instant.parse("2026-03-23T10:00:00Z"));

        RequestLogEntity success = log(11L, "GET", "/api/v1/foo", 200, "2026-03-23T10:05:00Z");
        RequestLogEntity auth = log(12L, "GET", "/api/v1/foo", 401, "2026-03-23T10:06:00Z");
        RequestLogEntity server = log(13L, "POST", "/api/v1/bar", 503, "2026-03-23T10:07:00Z");

        when(checkpointRepository.findById(DefaultEndpointRequestStatsRollupService.ROLLUP_JOB_NAME))
            .thenReturn(Optional.of(checkpoint));
        when(requestLogRepository.findByIdGreaterThanOrderByIdAsc(eq(10L), any(Pageable.class)))
            .thenReturn(List.of(success, auth, server));
        EndpointRequestStatsDailyRepository.MethodTemplatePairProjection pairGetFoo = pair("GET", "/api/v1/foo");
        EndpointRequestStatsDailyRepository.MethodTemplatePairProjection pairPostBar = pair("POST", "/api/v1/bar");
        when(statsRepository.findDistinctMethodTemplatePairs()).thenReturn(List.of(pairGetFoo, pairPostBar));
        when(statsRepository.findByBucketStartAndMethodAndTemplatePath(
            java.time.LocalDate.parse("2026-03-23"),
            "GET",
            "/api/v1/foo"
        )).thenReturn(Optional.empty());
        when(statsRepository.findByBucketStartAndMethodAndTemplatePath(
            java.time.LocalDate.parse("2026-03-23"),
            "POST",
            "/api/v1/bar"
        )).thenReturn(Optional.empty());
        when(statsRepository.save(any(EndpointRequestStatsDailyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultEndpointRequestStatsRollupService service = new DefaultEndpointRequestStatsRollupService(
            requestLogRepository,
            statsRepository,
            checkpointRepository,
            properties,
            clock
        );

        service.rollUpSinceLastCheckpoint();

        ArgumentCaptor<EndpointRequestStatsDailyEntity> statsCaptor = ArgumentCaptor.forClass(EndpointRequestStatsDailyEntity.class);
        verify(statsRepository, org.mockito.Mockito.times(4)).save(statsCaptor.capture());
        List<EndpointRequestStatsDailyEntity> saved = statsCaptor.getAllValues().stream().map(Objects::requireNonNull).toList();

        EndpointRequestStatsDailyEntity getFoo = saved
            .stream()
            .filter(it -> it.getTotalCount() > 0)
            .filter(it -> it.getMethod().equals("GET") && it.getTemplatePath().equals("/api/v1/foo"))
            .findFirst()
            .orElseThrow();
        assertThat(getFoo.getTotalCount()).isEqualTo(2);
        assertThat(getFoo.getSuccessCount()).isEqualTo(1);
        assertThat(getFoo.getAuthErrorCount()).isEqualTo(1);
        assertThat(getFoo.getClientErrorCount()).isEqualTo(0);
        assertThat(getFoo.getServerErrorCount()).isEqualTo(0);
        assertThat(getFoo.getOtherNonSuccessCount()).isEqualTo(0);

        EndpointRequestStatsDailyEntity postBar = saved
            .stream()
            .filter(it -> it.getTotalCount() > 0)
            .filter(it -> it.getMethod().equals("POST") && it.getTemplatePath().equals("/api/v1/bar"))
            .findFirst()
            .orElseThrow();
        assertThat(postBar.getTotalCount()).isEqualTo(1);
        assertThat(postBar.getSuccessCount()).isEqualTo(0);
        assertThat(postBar.getAuthErrorCount()).isEqualTo(0);
        assertThat(postBar.getClientErrorCount()).isEqualTo(0);
        assertThat(postBar.getServerErrorCount()).isEqualTo(1);
        assertThat(postBar.getOtherNonSuccessCount()).isEqualTo(0);

        ArgumentCaptor<CheckpointEntity> checkpointCaptor = ArgumentCaptor.forClass(
            CheckpointEntity.class
        );
        verify(checkpointRepository).save(checkpointCaptor.capture());
        CheckpointEntity savedCheckpoint = Objects.requireNonNull(checkpointCaptor.getValue());
        assertThat(savedCheckpoint.getLastProcessedRequestLogId()).isEqualTo(13L);
        assertThat(savedCheckpoint.getUpdatedAt()).isEqualTo(Instant.parse("2026-03-23T11:00:00Z"));
    }

    @Test
    void rollUpSinceLastCheckpoint_shouldRejectNonPositiveBatchSize() {
        RequestLogRepository requestLogRepository = mock(RequestLogRepository.class);
        EndpointRequestStatsDailyRepository statsRepository = mock(EndpointRequestStatsDailyRepository.class);
        ObservabilityRollupCheckpointRepository checkpointRepository = mock(ObservabilityRollupCheckpointRepository.class);
        RequestLogObservabilityProperties properties = properties(0);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC);

        DefaultEndpointRequestStatsRollupService service = new DefaultEndpointRequestStatsRollupService(
            requestLogRepository,
            statsRepository,
            checkpointRepository,
            properties,
            clock
        );

        assertThatThrownBy(service::rollUpSinceLastCheckpoint)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rollupBatchSize");
    }

    @Test
    void rollUpSinceLastCheckpoint_shouldCreateZeroRowsForKnownPairsWhenNoNewLogs() {
        RequestLogRepository requestLogRepository = mock(RequestLogRepository.class);
        EndpointRequestStatsDailyRepository statsRepository = mock(EndpointRequestStatsDailyRepository.class);
        ObservabilityRollupCheckpointRepository checkpointRepository = mock(ObservabilityRollupCheckpointRepository.class);
        RequestLogObservabilityProperties properties = properties(100);
        Clock clock = Clock.fixed(Instant.parse("2026-03-24T01:00:00Z"), ZoneOffset.UTC);

        CheckpointEntity checkpoint = new CheckpointEntity();
        checkpoint.setJobName(DefaultEndpointRequestStatsRollupService.ROLLUP_JOB_NAME);
        checkpoint.setLastProcessedRequestLogId(20L);
        checkpoint.setUpdatedAt(Instant.parse("2026-03-23T23:00:00Z"));

        EndpointRequestStatsDailyRepository.MethodTemplatePairProjection pairGetFoo = pair("GET", "/api/v1/foo");
        EndpointRequestStatsDailyRepository.MethodTemplatePairProjection pairPostBar = pair("POST", "/api/v1/bar");

        when(checkpointRepository.findById(DefaultEndpointRequestStatsRollupService.ROLLUP_JOB_NAME))
            .thenReturn(Optional.of(checkpoint));
        when(requestLogRepository.findByIdGreaterThanOrderByIdAsc(eq(20L), any(Pageable.class))).thenReturn(List.of());
        when(statsRepository.findDistinctMethodTemplatePairs()).thenReturn(List.of(pairGetFoo, pairPostBar));
        when(statsRepository.findByBucketStartAndMethodAndTemplatePath(
            java.time.LocalDate.parse("2026-03-24"),
            "GET",
            "/api/v1/foo"
        )).thenReturn(Optional.empty());
        when(statsRepository.findByBucketStartAndMethodAndTemplatePath(
            java.time.LocalDate.parse("2026-03-24"),
            "POST",
            "/api/v1/bar"
        )).thenReturn(Optional.empty());
        when(statsRepository.save(any(EndpointRequestStatsDailyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DefaultEndpointRequestStatsRollupService service = new DefaultEndpointRequestStatsRollupService(
            requestLogRepository,
            statsRepository,
            checkpointRepository,
            properties,
            clock
        );

        service.rollUpSinceLastCheckpoint();

        ArgumentCaptor<EndpointRequestStatsDailyEntity> statsCaptor = ArgumentCaptor.forClass(EndpointRequestStatsDailyEntity.class);
        verify(statsRepository, org.mockito.Mockito.times(2)).save(statsCaptor.capture());
        List<EndpointRequestStatsDailyEntity> saved = statsCaptor.getAllValues().stream().map(Objects::requireNonNull).toList();
        assertThat(saved).allSatisfy(stat -> {
            assertThat(stat.getBucketStart()).isEqualTo(java.time.LocalDate.parse("2026-03-24"));
            assertThat(stat.getTotalCount()).isEqualTo(0);
            assertThat(stat.getSuccessCount()).isEqualTo(0);
            assertThat(stat.getAuthErrorCount()).isEqualTo(0);
            assertThat(stat.getClientErrorCount()).isEqualTo(0);
            assertThat(stat.getServerErrorCount()).isEqualTo(0);
            assertThat(stat.getOtherNonSuccessCount()).isEqualTo(0);
        });

        verify(checkpointRepository, never()).save(any(CheckpointEntity.class));
    }

    private static RequestLogEntity log(Long id, String method, String templatePath, int status, String createdAt) {
        RequestLogEntity entity = new RequestLogEntity();
        entity.setId(id);
        entity.setMethod(method);
        entity.setTemplatePath(templatePath);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.parse(createdAt));
        return entity;
    }

    private static RequestLogObservabilityProperties properties(int rollupBatchSize) {
        RequestLogObservabilityProperties properties = new RequestLogObservabilityProperties();
        properties.setRollupBatchSize(rollupBatchSize);
        return properties;
    }

    private static EndpointRequestStatsDailyRepository.MethodTemplatePairProjection pair(String method, String templatePath) {
        EndpointRequestStatsDailyRepository.MethodTemplatePairProjection projection = mock(
            EndpointRequestStatsDailyRepository.MethodTemplatePairProjection.class
        );
        when(projection.getMethod()).thenReturn(method);
        when(projection.getTemplatePath()).thenReturn(templatePath);
        return projection;
    }
}
