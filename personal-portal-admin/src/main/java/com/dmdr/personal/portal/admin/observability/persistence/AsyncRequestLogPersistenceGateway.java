package com.dmdr.personal.portal.admin.observability.persistence;

import com.dmdr.personal.portal.admin.observability.RequestLogObservabilityProperties;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import static com.dmdr.personal.portal.admin.observability.RequestLogObservabilityAutoConfiguration.REQUEST_LOG_TASK_EXECUTOR_BEAN_NAME;

/**
 * Best-effort async gateway that may drop in-memory records under load or shutdown.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C5).
 */
@Slf4j
@Component
public class AsyncRequestLogPersistenceGateway implements RequestLogPersistenceGateway {

    private final Executor executor;
    private final RequestLogWriter requestLogWriter;
    private final int flushBatchSize;
    private final long flushIntervalMs;
    private final Object bufferLock = new Object();
    private final List<RequestLogRecord> buffer;
    private final ScheduledExecutorService flushScheduler;

    public AsyncRequestLogPersistenceGateway(
        @Qualifier(REQUEST_LOG_TASK_EXECUTOR_BEAN_NAME) Executor executor,
        RequestLogWriter requestLogWriter,
        RequestLogObservabilityProperties properties
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.requestLogWriter = Objects.requireNonNull(requestLogWriter, "requestLogWriter must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.flushBatchSize = positive(properties.getAsyncFlushBatchSize(), "asyncFlushBatchSize");
        this.flushIntervalMs = positiveLong(properties.getAsyncFlushIntervalMs(), "asyncFlushIntervalMs");
        this.buffer = new ArrayList<>(flushBatchSize);
        this.flushScheduler = Executors.newSingleThreadScheduledExecutor(new FlushThreadFactory());
        this.flushScheduler.scheduleWithFixedDelay(
            this::flushByTime,
            flushIntervalMs,
            flushIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void enqueue(RequestLogRecord record) {
        log.info("Enqueuing request-log record: {}", record);
        if (record == null) {
            return;
        }
        boolean shouldFlushBySize = false;
        synchronized (bufferLock) {
            buffer.add(record);
            shouldFlushBySize = buffer.size() >= flushBatchSize;
        }
        if (shouldFlushBySize) {
            try {
                executor.execute(this::flushBySize);
            } catch (RuntimeException exception) {
                log.warn("Request-log flush task scheduling failed; keeping records buffered for timed flush", exception);
            }
        }
    }

    @PreDestroy
    void flushOnShutdown() {
        flushScheduler.shutdownNow();
        persistBuffer("shutdown");
    }

    private void flushByTime() {
        persistBuffer("time");
    }

    private void flushBySize() {
        persistBuffer("size");
    }

    private void persistBuffer(String reason) {
        List<RequestLogRecord> toPersist;
        synchronized (bufferLock) {
            if (buffer.isEmpty()) {
                return;
            }
            toPersist = new ArrayList<>(buffer);
            buffer.clear();
        }
        try {
            requestLogWriter.persistBatch(toPersist);
            log.debug("Flushed request-log batch: reason={}, size={}", reason, toPersist.size());
        } catch (Exception exception) {
            log.warn(
                "Request-log batch persistence failed; dropping {} in-memory records (reason={})",
                toPersist.size(),
                reason,
                exception
            );
        }
    }

    private static int positive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return value;
    }

    private static long positiveLong(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return value;
    }

    private static final class FlushThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "request-log-flush-scheduler");
            thread.setDaemon(true);
            return thread;
        }
    }
}
