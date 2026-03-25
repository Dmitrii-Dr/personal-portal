package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.api.mapper.DefaultEndpointStatsDailyMapper;
import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsDailyResponse;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DefaultEndpointStatsDailyMapperTest {

    private final DefaultEndpointStatsDailyMapper mapper = new DefaultEndpointStatsDailyMapper();

    @Test
    void toResponse_shouldMapAllFields() {
        EndpointRequestStatsDailyEntity entity = new EndpointRequestStatsDailyEntity();
        entity.setBucketStart(LocalDate.parse("2026-03-22"));
        entity.setMethod("GET");
        entity.setTemplatePath("/api/v1/articles/{id}");
        entity.setTotalCount(10);
        entity.setSuccessCount(6);
        entity.setAuthErrorCount(1);
        entity.setClientErrorCount(2);
        entity.setServerErrorCount(1);
        entity.setOtherNonSuccessCount(0);

        EndpointStatsDailyResponse response = mapper.toResponse(entity);

        assertThat(response.bucketStart()).isEqualTo(LocalDate.parse("2026-03-22"));
        assertThat(response.method()).isEqualTo("GET");
        assertThat(response.templatePath()).isEqualTo("/api/v1/articles/{id}");
        assertThat(response.totalCount()).isEqualTo(10);
        assertThat(response.successCount()).isEqualTo(6);
        assertThat(response.authErrorCount()).isEqualTo(1);
        assertThat(response.clientErrorCount()).isEqualTo(2);
        assertThat(response.serverErrorCount()).isEqualTo(1);
        assertThat(response.otherNonSuccessCount()).isEqualTo(0);
    }
}
