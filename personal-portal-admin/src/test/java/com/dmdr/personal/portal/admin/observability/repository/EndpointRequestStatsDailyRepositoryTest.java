package com.dmdr.personal.portal.admin.observability.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = EndpointRequestStatsDailyRepositoryTest.JpaSliceConfig.class)
class EndpointRequestStatsDailyRepositoryTest {

    @Autowired
    private EndpointRequestStatsDailyRepository repository;

    @Test
    @SuppressWarnings("null")
    void findTopErrorEndpoints_shouldAggregateSortFilterAndPaginate() {
        repository.save(createRow(LocalDate.parse("2026-03-01"), "GET", "/api/v1/a", 10, 4, 1, 2, 3, 0));
        repository.save(createRow(LocalDate.parse("2026-03-02"), "GET", "/api/v1/a", 5, 2, 0, 1, 2, 0));
        repository.save(createRow(LocalDate.parse("2026-03-01"), "POST", "/api/v1/b", 8, 2, 3, 2, 1, 0));
        repository.save(createRow(LocalDate.parse("2026-03-01"), "PUT", "/api/v1/c", 3, 3, 0, 0, 0, 0));
        repository.save(createRow(LocalDate.parse("2026-02-28"), "DELETE", "/api/v1/d", 9, 1, 2, 2, 2, 2));

        var page0 = repository.findTopErrorEndpoints(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            PageRequest.of(0, 1)
        );
        var page1 = repository.findTopErrorEndpoints(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            PageRequest.of(1, 1)
        );

        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(page0.getContent()).hasSize(1);
        assertThat(page0.getContent().get(0).getMethod()).isEqualTo("GET");
        assertThat(page0.getContent().get(0).getTemplatePath()).isEqualTo("/api/v1/a");
        assertThat(page0.getContent().get(0).getTotalErrorCount()).isEqualTo(9L);
        assertThat(page0.getContent().get(0).getTotalCount()).isEqualTo(15L);
        assertThat(page0.getContent().get(0).getSuccessCount()).isEqualTo(6L);

        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getMethod()).isEqualTo("POST");
        assertThat(page1.getContent().get(0).getTemplatePath()).isEqualTo("/api/v1/b");
        assertThat(page1.getContent().get(0).getTotalErrorCount()).isEqualTo(6L);
    }

    private static EndpointRequestStatsDailyEntity createRow(
        LocalDate bucketStart,
        String method,
        String templatePath,
        long totalCount,
        long successCount,
        long authErrorCount,
        long clientErrorCount,
        long serverErrorCount,
        long otherNonSuccessCount
    ) {
        EndpointRequestStatsDailyEntity entity = new EndpointRequestStatsDailyEntity();
        entity.setBucketStart(bucketStart);
        entity.setMethod(method);
        entity.setTemplatePath(templatePath);
        entity.setTotalCount(totalCount);
        entity.setSuccessCount(successCount);
        entity.setAuthErrorCount(authErrorCount);
        entity.setClientErrorCount(clientErrorCount);
        entity.setServerErrorCount(serverErrorCount);
        entity.setOtherNonSuccessCount(otherNonSuccessCount);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = EndpointRequestStatsDailyEntity.class)
    @EnableJpaRepositories(basePackageClasses = EndpointRequestStatsDailyRepository.class)
    static class JpaSliceConfig {
    }
}
