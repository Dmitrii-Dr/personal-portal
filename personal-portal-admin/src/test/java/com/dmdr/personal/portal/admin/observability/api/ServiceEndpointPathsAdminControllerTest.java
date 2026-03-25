package com.dmdr.personal.portal.admin.observability.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.admin.observability.api.controller.ServiceEndpointPathsAdminController;
import com.dmdr.personal.portal.admin.observability.service.ServiceEndpointPathsQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceEndpointPathsAdminControllerTest {

    @Test
    void list_shouldDelegateToQueryServiceAndReturnPaths() {
        ServiceEndpointPathsQueryService queryService = mock(ServiceEndpointPathsQueryService.class);
        ServiceEndpointPathsAdminController controller = new ServiceEndpointPathsAdminController(queryService);
        List<String> paths = List.of("/api/v1/public/articles", "/api/v1/public/articles/{id}");
        when(queryService.listPaths()).thenReturn(paths);

        List<String> response = controller.list();

        assertThat(response).containsExactly("/api/v1/public/articles", "/api/v1/public/articles/{id}");
        verify(queryService).listPaths();
    }
}
