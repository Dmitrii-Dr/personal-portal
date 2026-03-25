package com.dmdr.personal.portal.admin.observability.api.controller;

import com.dmdr.personal.portal.admin.observability.service.ServiceEndpointPathsQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint contract in docs/observability/admin-observability-public-api.md.
 */
@RestController
@RequestMapping("/api/v1/admin/observability/endpoint-stats/paths")
public class ServiceEndpointPathsAdminController {

    private final ServiceEndpointPathsQueryService queryService;

    public ServiceEndpointPathsAdminController(ServiceEndpointPathsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public List<String> list() {
        return queryService.listPaths();
    }
}
