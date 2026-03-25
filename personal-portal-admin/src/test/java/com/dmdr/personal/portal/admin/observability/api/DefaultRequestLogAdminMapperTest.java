package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.api.mapper.DefaultRequestLogAdminMapper;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogDetailResponse;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogListItemResponse;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultRequestLogAdminMapperTest {

    private final DefaultRequestLogAdminMapper mapper = new DefaultRequestLogAdminMapper();

    @Test
    void toListItem_shouldExcludeStackTrace() {
        RequestLogEntity entity = sampleEntity();

        RequestLogListItemResponse response = mapper.toListItem(entity);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.templatePath()).isEqualTo("/api/v1/auth/login");
        assertThat(response.method()).isEqualTo("POST");
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.durationMs()).isEqualTo(25L);
        assertThat(response.errorCode()).isEqualTo("AUTH");
        assertThat(response.errorMessage()).isEqualTo("Bad credentials");
    }

    @Test
    void toDetail_shouldIncludeStackTrace() {
        RequestLogEntity entity = sampleEntity();

        RequestLogDetailResponse response = mapper.toDetail(entity);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.requestBody()).isEqualTo("{\"email\":\"***\"}");
        assertThat(response.requestHeaders()).isEqualTo("{\"Authorization\":[\"***\"]}");
        assertThat(response.responseHeaders()).isEqualTo("{\"Set-Cookie\":[\"***\"]}");
        assertThat(response.stackTrace()).isEqualTo("trace");
    }

    private static RequestLogEntity sampleEntity() {
        RequestLogEntity entity = new RequestLogEntity();
        entity.setId(7L);
        entity.setPath("/api/v1/auth/login");
        entity.setTemplatePath("/api/v1/auth/login");
        entity.setMethod("POST");
        entity.setStatus(401);
        entity.setDurationMs(25L);
        entity.setUserId(UUID.randomUUID());
        entity.setCreatedAt(Instant.parse("2026-03-23T10:15:30Z"));
        entity.setErrorCode("AUTH");
        entity.setErrorMessage("Bad credentials");
        entity.setRequestBody("{\"email\":\"***\"}");
        entity.setRequestHeaders("{\"Authorization\":[\"***\"]}");
        entity.setResponseHeaders("{\"Set-Cookie\":[\"***\"]}");
        entity.setStackTrace("trace");
        return entity;
    }
}
