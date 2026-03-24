package com.dmdr.personal.portal.admin.observability.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class RepositoryRequestLogWriterTest {

    @Test
    @SuppressWarnings("null")
    void persistBatch_shouldMapRecordAndUseUtcClockForCreatedAt() {
        RequestLogRepository repository = mock(RequestLogRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneOffset.UTC);
        RepositoryRequestLogWriter writer = new RepositoryRequestLogWriter(repository, clock);
        RequestLogRecord record = new RequestLogRecord(
            "/api/articles/7",
            "/api/articles/{id}",
            "PATCH",
            422,
            35L,
            UUID.randomUUID(),
            Instant.parse("2020-01-01T00:00:00Z"),
            "VALIDATION_FAILED",
            "Bad field",
            "{\"email\":\"***\"}",
            "{\"Authorization\":[\"***\"]}",
            "{\"Set-Cookie\":[\"***\"]}",
            "stack"
        );
        writer.persistBatch(List.of(record));

        verify(repository).saveAll(
            argThat(entities -> {
                RequestLogEntity entity = entities.iterator().next();
                assertThat(entity.getPath()).isEqualTo("/api/articles/7");
                assertThat(entity.getTemplatePath()).isEqualTo("/api/articles/{id}");
                assertThat(entity.getMethod()).isEqualTo("PATCH");
                assertThat(entity.getStatus()).isEqualTo(422);
                assertThat(entity.getDurationMs()).isEqualTo(35L);
                assertThat(entity.getUserId()).isEqualTo(record.userId());
                assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2026-03-23T15:00:00Z"));
                assertThat(entity.getErrorCode()).isEqualTo("VALIDATION_FAILED");
                assertThat(entity.getErrorMessage()).isEqualTo("Bad field");
                assertThat(entity.getRequestBody()).isEqualTo("{\"email\":\"***\"}");
                assertThat(entity.getRequestHeaders()).isEqualTo("{\"Authorization\":[\"***\"]}");
                assertThat(entity.getResponseHeaders()).isEqualTo("{\"Set-Cookie\":[\"***\"]}");
                assertThat(entity.getStackTrace()).isEqualTo("stack");
                return true;
            })
        );
    }
}
