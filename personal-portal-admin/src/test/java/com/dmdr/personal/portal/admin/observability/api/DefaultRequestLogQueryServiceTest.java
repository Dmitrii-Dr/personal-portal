package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import com.dmdr.personal.portal.admin.observability.repository.RequestLogRepository;
import java.time.Instant;

import com.dmdr.personal.portal.admin.observability.service.DefaultRequestLogQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class DefaultRequestLogQueryServiceTest {

    @Test
    void search_shouldDelegateToRepositoryWithSpecifications() {
        RequestLogRepository repository = mock(RequestLogRepository.class);
        DefaultRequestLogQueryService service = new DefaultRequestLogQueryService(repository);
        PageRequest pageable = PageRequest.of(0, 20);
        RequestLogQuery query = new RequestLogQuery(
            Instant.parse("2026-03-20T00:00:00Z"),
            Instant.parse("2026-03-21T00:00:00Z"),
            500,
            "/api/v1/auth/login",
            "POST",
            null,
            "ERR",
            "boom",
            pageable
        );
        Page<RequestLogEntity> page = new PageImpl<>(java.util.List.of(new RequestLogEntity()));
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);

        Page<RequestLogEntity> result = service.search(query);

        assertThat(result).isSameAs(page);
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }
}
