package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsPeriodResponse;
import com.dmdr.personal.portal.admin.observability.api.response.TopErrorEndpointResponse;
import com.dmdr.personal.portal.admin.observability.api.controller.EndpointDailyStatsAdminController;
import com.dmdr.personal.portal.admin.observability.api.mapper.EndpointStatsDailyMapper;
import com.dmdr.personal.portal.admin.observability.service.EndpointStatsDailyQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EndpointDailyStatsAdminControllerTest {

    @Test
    void list_shouldCapPageSizeTo100() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);
        when(queryService.search(any())).thenReturn(Page.empty());

        controller.list(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-10"),
            List.of("GET"),
            List.of("/api/v1/public/articles"),
            PageRequest.of(0, 500, Sort.by("bucketStart").descending())
        );

        ArgumentCaptor<EndpointStatsDailyQuery> queryCaptor = ArgumentCaptor.forClass(EndpointStatsDailyQuery.class);
        verify(queryService).search(queryCaptor.capture());
        assertThat(queryCaptor.getValue().pageable().getPageSize()).isEqualTo(100);
    }

    @Test
    void list_shouldRejectInvalidDateRange() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);

        assertThatThrownBy(() -> controller.list(
            LocalDate.parse("2026-03-10"),
            LocalDate.parse("2026-03-01"),
            List.of("GET"),
            List.of("/api/v1/public/articles"),
            PageRequest.of(0, 20)
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void period_shouldRejectInvalidDateRange() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);

        assertThatThrownBy(() -> controller.period(
            LocalDate.parse("2026-09-16"),
            LocalDate.parse("2026-09-15"),
            null,
            null
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void period_shouldAllowExactlyOneCalendarMonth() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);
        when(queryService.searchPeriod(any())).thenReturn(List.of());

        controller.period(
            LocalDate.parse("2026-08-15"),
            LocalDate.parse("2026-09-15"),
            List.of("GET"),
            List.of("/api/v1/public/articles")
        );

        verify(queryService).searchPeriod(any());
    }

    @Test
    void period_shouldRejectMoreThanOneCalendarMonth() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);

        assertThatThrownBy(() -> controller.period(
            LocalDate.parse("2026-08-15"),
            LocalDate.parse("2026-09-16"),
            null,
            null
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void period_shouldNormalizeListsAndPassToService() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);
        when(queryService.searchPeriod(any())).thenReturn(List.of(new EndpointStatsPeriodResponse("GET", null, 1, 1, 0, 0, 0, 0)));

        controller.period(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            List.of("get", " GET ", ""),
            List.of(" /api/v1/public/articles ", "")
        );

        ArgumentCaptor<EndpointStatsPeriodQuery> queryCaptor = ArgumentCaptor.forClass(EndpointStatsPeriodQuery.class);
        verify(queryService).searchPeriod(queryCaptor.capture());
        EndpointStatsPeriodQuery query = queryCaptor.getValue();
        assertThat(query.methods()).containsExactly("GET");
        assertThat(query.templatePaths()).containsExactly("/api/v1/public/articles");
    }

    @Test
    @SuppressWarnings("null")
    void topErrors_shouldCapPageSizeTo20() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);
        when(queryService.searchTopErrors(any())).thenReturn(new PageImpl<>(List.of()));

        controller.topErrors(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            PageRequest.of(0, 100)
        );

        ArgumentCaptor<EndpointTopErrorsQuery> queryCaptor = ArgumentCaptor.forClass(EndpointTopErrorsQuery.class);
        verify(queryService).searchTopErrors(queryCaptor.capture());
        assertThat(queryCaptor.getValue().pageable().getPageSize()).isEqualTo(20);
    }

    @Test
    void topErrors_shouldRejectInvalidDateRange() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);

        assertThatThrownBy(() -> controller.topErrors(
            LocalDate.parse("2026-09-16"),
            LocalDate.parse("2026-09-15"),
            PageRequest.of(0, 20)
        ))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @SuppressWarnings("null")
    void topErrors_shouldPassQueryToService() {
        EndpointStatsDailyQueryService queryService = mock(EndpointStatsDailyQueryService.class);
        EndpointStatsDailyMapper mapper = mock(EndpointStatsDailyMapper.class);
        EndpointDailyStatsAdminController controller = new EndpointDailyStatsAdminController(queryService, mapper);
        when(queryService.searchTopErrors(any())).thenReturn(new PageImpl<>(List.of(
            new TopErrorEndpointResponse("GET", "/api/v1/public/articles", 10, 8, 0, 1, 1, 0, 2)
        )));

        controller.topErrors(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            PageRequest.of(1, 10)
        );

        ArgumentCaptor<EndpointTopErrorsQuery> queryCaptor = ArgumentCaptor.forClass(EndpointTopErrorsQuery.class);
        verify(queryService).searchTopErrors(queryCaptor.capture());
        EndpointTopErrorsQuery query = queryCaptor.getValue();
        assertThat(query.from()).isEqualTo(LocalDate.parse("2026-03-01"));
        assertThat(query.to()).isEqualTo(LocalDate.parse("2026-03-31"));
        assertThat(query.pageable().getPageNumber()).isEqualTo(1);
        assertThat(query.pageable().getPageSize()).isEqualTo(10);
    }
}
