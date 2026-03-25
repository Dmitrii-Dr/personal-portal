package com.dmdr.personal.portal.admin.observability.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * API contract from docs/observability/admin-observability-public-api.md.
 */
@Service
public class ServiceEndpointPathsQueryService {

    private final ServiceEndpointPathsCatalog serviceEndpointPathsCatalog;

    public ServiceEndpointPathsQueryService(ServiceEndpointPathsCatalog serviceEndpointPathsCatalog) {
        this.serviceEndpointPathsCatalog = serviceEndpointPathsCatalog;
    }

    public List<String> listPaths() {
        return serviceEndpointPathsCatalog.paths();
    }
}
