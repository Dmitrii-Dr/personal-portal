package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.api.controller.RequestLogAdminController;
import com.dmdr.personal.portal.admin.observability.api.mapper.RequestLogAdminMapper;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogDetailResponse;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import java.time.Instant;
import java.util.Optional;

import com.dmdr.personal.portal.admin.observability.service.RequestLogQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RequestLogAdminControllerTest {

    @Test
    void list_shouldCapPageSizeTo100() {
        RequestLogQueryService queryService = mock(RequestLogQueryService.class);
        RequestLogAdminMapper mapper = mock(RequestLogAdminMapper.class);
        RequestLogAdminController controller = new RequestLogAdminController(queryService, mapper);
        when(queryService.search(any())).thenReturn(new PageImpl<>(java.util.List.of()));

        controller.list(
            Instant.parse("2026-03-20T00:00:00Z"),
            Instant.parse("2026-03-20T12:00:00Z"),
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 500, Sort.by("createdAt").descending())
        );

        ArgumentCaptor<RequestLogQuery> queryCaptor = ArgumentCaptor.forClass(RequestLogQuery.class);
        verify(queryService).search(queryCaptor.capture());
        assertThat(queryCaptor.getValue().pageable().getPageSize()).isEqualTo(100);
    }

    @Test
    void list_shouldRejectInvalidRange() {
        RequestLogQueryService queryService = mock(RequestLogQueryService.class);
        RequestLogAdminMapper mapper = mock(RequestLogAdminMapper.class);
        RequestLogAdminController controller = new RequestLogAdminController(queryService, mapper);
        Instant time = Instant.parse("2026-03-20T00:00:00Z");

        assertThatThrownBy(() -> controller.list(
            time,
            time,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void detail_shouldReturn404WhenMissing() {
        RequestLogQueryService queryService = mock(RequestLogQueryService.class);
        RequestLogAdminMapper mapper = mock(RequestLogAdminMapper.class);
        RequestLogAdminController controller = new RequestLogAdminController(queryService, mapper);
        when(queryService.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.detail(77L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void detail_shouldMapEntityToResponse() {
        RequestLogQueryService queryService = mock(RequestLogQueryService.class);
        RequestLogAdminMapper mapper = mock(RequestLogAdminMapper.class);
        RequestLogAdminController controller = new RequestLogAdminController(queryService, mapper);
        RequestLogEntity entity = new RequestLogEntity();
        RequestLogDetailResponse response = new RequestLogDetailResponse(
            1L,
            "/a",
            "/a",
            "GET",
            200,
            1L,
            null,
            Instant.parse("2026-03-20T00:00:00Z"),
            null,
            null,
            null,
            null,
            null,
            null
        );
        when(queryService.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDetail(entity)).thenReturn(response);

        RequestLogDetailResponse result = controller.detail(1L);

        assertThat(result).isSameAs(response);
    }
}
