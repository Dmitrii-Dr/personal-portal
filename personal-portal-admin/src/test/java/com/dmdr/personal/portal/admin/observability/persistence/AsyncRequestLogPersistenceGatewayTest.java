package com.dmdr.personal.portal.admin.observability.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class AsyncRequestLogPersistenceGatewayTest {

    @Test
    void enqueue_shouldFlushByBatchSize() {
        RequestLogWriter requestLogWriter = mock(RequestLogWriter.class);
        Executor directExecutor = Runnable::run;
        RequestLogObservabilityProperties properties = properties(2, 60_000);
        AsyncRequestLogPersistenceGateway gateway = new AsyncRequestLogPersistenceGateway(directExecutor, requestLogWriter, properties);

        RequestLogRecord record = new RequestLogRecord(
            "/api/articles/42",
            "/api/articles/{id}",
            "GET",
            200,
            14L,
            UUID.randomUUID(),
            Instant.parse("2026-03-23T10:15:30Z"),
            null,
            null,
            null,
            null,
            null,
            null
        );

        gateway.enqueue(record);
        gateway.enqueue(record);

        verify(requestLogWriter).persistBatch(List.of(record, record));
        gateway.flushOnShutdown();
    }

    @Test
    void enqueue_shouldFlushByTimeInterval() {
        RequestLogWriter requestLogWriter = mock(RequestLogWriter.class);
        RequestLogObservabilityProperties properties = properties(100, 20);
        AsyncRequestLogPersistenceGateway gateway = new AsyncRequestLogPersistenceGateway(Runnable::run, requestLogWriter, properties);

        RequestLogRecord record = new RequestLogRecord(
            "/api/time",
            "/api/time",
            "GET",
            200,
            5L,
            null,
            Instant.parse("2026-03-23T10:15:30Z"),
            null,
            null,
            null,
            null,
            null,
            null
        );
        gateway.enqueue(record);

        verify(requestLogWriter, timeout(500)).persistBatch(List.of(record));
        gateway.flushOnShutdown();
    }

    @Test
    void enqueue_shouldSwallowWriterFailure() {
        RequestLogWriter requestLogWriter = mock(RequestLogWriter.class);
        RequestLogRecord record = new RequestLogRecord(
            "/api/fail",
            "/api/fail",
            "POST",
            500,
            4L,
            null,
            Instant.parse("2026-03-23T10:15:30Z"),
            "ERR",
            "boom",
            null,
            null,
            null,
            "trace"
        );
        doThrow(new RuntimeException("db down")).when(requestLogWriter).persistBatch(List.of(record, record));
        RequestLogObservabilityProperties properties = properties(2, 60_000);
        AsyncRequestLogPersistenceGateway gateway = new AsyncRequestLogPersistenceGateway(Runnable::run, requestLogWriter, properties);

        gateway.enqueue(record);
        gateway.enqueue(record);

        assertThatCode(gateway::flushOnShutdown).doesNotThrowAnyException();
    }

    private static RequestLogObservabilityProperties properties(int flushBatchSize, long flushIntervalMs) {
        RequestLogObservabilityProperties properties = new RequestLogObservabilityProperties();
        properties.setAsyncFlushBatchSize(flushBatchSize);
        properties.setAsyncFlushIntervalMs(flushIntervalMs);
        return properties;
    }
}
