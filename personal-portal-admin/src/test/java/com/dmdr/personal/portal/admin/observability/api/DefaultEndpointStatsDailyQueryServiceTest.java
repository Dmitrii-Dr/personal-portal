package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.repository.EndpointRequestStatsDailyRepository;
import com.dmdr.personal.portal.admin.observability.service.DefaultEndpointStatsDailyQueryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class DefaultEndpointStatsDailyQueryServiceTest {

    @Test
    void searchPeriod_shouldGroupByMethodAndTemplatePathWhenBothFiltersPresent() {
        EndpointRequestStatsDailyRepository repository = mock(EndpointRequestStatsDailyRepository.class);
        DefaultEndpointStatsDailyQueryService service = new DefaultEndpointStatsDailyQueryService(repository);
        EndpointRequestStatsDailyRepository.GroupedPeriodStatsProjection row = mock(
            EndpointRequestStatsDailyRepository.GroupedPeriodStatsProjection.class
        );
        when(row.getMethod()).thenReturn("GET");
        when(row.getTemplatePath()).thenReturn("/api/v1/public/articles");
        when(row.getTotalCount()).thenReturn(10L);
        when(row.getSuccessCount()).thenReturn(8L);
        when(row.getAuthErrorCount()).thenReturn(1L);
        when(row.getClientErrorCount()).thenReturn(1L);
        when(row.getServerErrorCount()).thenReturn(0L);
        when(row.getOtherNonSuccessCount()).thenReturn(0L);
        when(
            repository.aggregatePeriodByMethodAndTemplatePath(
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                List.of("GET"),
                List.of("/api/v1/public/articles")
            )
        ).thenReturn(List.of(row));

        var result = service.searchPeriod(
            new EndpointStatsPeriodQuery(
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                List.of("GET"),
                List.of("/api/v1/public/articles")
            )
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).method()).isEqualTo("GET");
        assertThat(result.get(0).templatePath()).isEqualTo("/api/v1/public/articles");
        assertThat(result.get(0).successCount()).isEqualTo(8L);
        verify(repository).aggregatePeriodByMethodAndTemplatePath(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            List.of("GET"),
            List.of("/api/v1/public/articles")
        );
    }

    @Test
    void searchPeriod_shouldGroupByMethodWhenOnlyMethodsFilterPresent() {
        EndpointRequestStatsDailyRepository repository = mock(EndpointRequestStatsDailyRepository.class);
        DefaultEndpointStatsDailyQueryService service = new DefaultEndpointStatsDailyQueryService(repository);
        EndpointRequestStatsDailyRepository.GroupedPeriodStatsByMethodProjection row = mock(
            EndpointRequestStatsDailyRepository.GroupedPeriodStatsByMethodProjection.class
        );
        when(row.getMethod()).thenReturn("POST");
        when(row.getTotalCount()).thenReturn(7L);
        when(row.getSuccessCount()).thenReturn(2L);
        when(row.getAuthErrorCount()).thenReturn(1L);
        when(row.getClientErrorCount()).thenReturn(2L);
        when(row.getServerErrorCount()).thenReturn(2L);
        when(row.getOtherNonSuccessCount()).thenReturn(0L);
        when(repository.aggregatePeriodByMethod(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31"), List.of("POST")))
            .thenReturn(List.of(row));

        var result = service.searchPeriod(
            new EndpointStatsPeriodQuery(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31"), List.of("POST"), null)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).method()).isEqualTo("POST");
        assertThat(result.get(0).templatePath()).isNull();
        assertThat(result.get(0).totalCount()).isEqualTo(7L);
        verify(repository).aggregatePeriodByMethod(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            List.of("POST")
        );
    }

    @Test
    void searchPeriod_shouldGroupByTemplatePathWhenOnlyTemplatePathsPresent() {
        EndpointRequestStatsDailyRepository repository = mock(EndpointRequestStatsDailyRepository.class);
        DefaultEndpointStatsDailyQueryService service = new DefaultEndpointStatsDailyQueryService(repository);
        EndpointRequestStatsDailyRepository.GroupedPeriodStatsByTemplatePathProjection row = mock(
            EndpointRequestStatsDailyRepository.GroupedPeriodStatsByTemplatePathProjection.class
        );
        when(row.getTemplatePath()).thenReturn("/api/v1/auth/login");
        when(row.getTotalCount()).thenReturn(20L);
        when(row.getSuccessCount()).thenReturn(10L);
        when(row.getAuthErrorCount()).thenReturn(3L);
        when(row.getClientErrorCount()).thenReturn(5L);
        when(row.getServerErrorCount()).thenReturn(2L);
        when(row.getOtherNonSuccessCount()).thenReturn(0L);
        when(
            repository.aggregatePeriodByTemplatePath(
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                List.of("/api/v1/auth/login")
            )
        ).thenReturn(List.of(row));

        var result = service.searchPeriod(
            new EndpointStatsPeriodQuery(
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2026-03-31"),
                null,
                List.of("/api/v1/auth/login")
            )
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).method()).isNull();
        assertThat(result.get(0).templatePath()).isEqualTo("/api/v1/auth/login");
        assertThat(result.get(0).totalCount()).isEqualTo(20L);
        verify(repository).aggregatePeriodByTemplatePath(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            List.of("/api/v1/auth/login")
        );
    }

    @Test
    void searchPeriod_shouldReturnEmptyWhenNoFiltersProvided() {
        EndpointRequestStatsDailyRepository repository = mock(EndpointRequestStatsDailyRepository.class);
        DefaultEndpointStatsDailyQueryService service = new DefaultEndpointStatsDailyQueryService(repository);

        var result = service.searchPeriod(new EndpointStatsPeriodQuery(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            null,
            null
        ));

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("null")
    void searchTopErrors_shouldMapRepositoryProjectionToResponse() {
        EndpointRequestStatsDailyRepository repository = mock(EndpointRequestStatsDailyRepository.class);
        DefaultEndpointStatsDailyQueryService service = new DefaultEndpointStatsDailyQueryService(repository);
        EndpointRequestStatsDailyRepository.TopErrorEndpointProjection row = mock(
            EndpointRequestStatsDailyRepository.TopErrorEndpointProjection.class
        );
        when(row.getMethod()).thenReturn("POST");
        when(row.getTemplatePath()).thenReturn("/api/v1/admin/tasks");
        when(row.getTotalCount()).thenReturn(30L);
        when(row.getSuccessCount()).thenReturn(10L);
        when(row.getAuthErrorCount()).thenReturn(5L);
        when(row.getClientErrorCount()).thenReturn(8L);
        when(row.getServerErrorCount()).thenReturn(6L);
        when(row.getOtherNonSuccessCount()).thenReturn(1L);
        when(row.getTotalErrorCount()).thenReturn(20L);
        when(repository.findTopErrorEndpoints(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        var result = service.searchTopErrors(
            new EndpointTopErrorsQuery(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31"), PageRequest.of(0, 20))
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).method()).isEqualTo("POST");
        assertThat(result.getContent().get(0).templatePath()).isEqualTo("/api/v1/admin/tasks");
        assertThat(result.getContent().get(0).totalCount()).isEqualTo(30L);
        assertThat(result.getContent().get(0).successCount()).isEqualTo(10L);
        assertThat(result.getContent().get(0).authErrorCount()).isEqualTo(5L);
        assertThat(result.getContent().get(0).clientErrorCount()).isEqualTo(8L);
        assertThat(result.getContent().get(0).serverErrorCount()).isEqualTo(6L);
        assertThat(result.getContent().get(0).otherNonSuccessCount()).isEqualTo(1L);
        assertThat(result.getContent().get(0).totalErrorCount()).isEqualTo(20L);
    }
}
