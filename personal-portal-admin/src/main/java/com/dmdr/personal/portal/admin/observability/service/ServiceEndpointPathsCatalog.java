package com.dmdr.personal.portal.admin.observability.service;

import com.dmdr.personal.portal.admin.observability.routing.RequestLoggingPathPolicy;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Startup endpoint catalog for docs/observability/admin-observability-public-api.md.
 */
@Component
public class ServiceEndpointPathsCatalog {

    private final List<String> paths;

    public ServiceEndpointPathsCatalog(
        RequestMappingHandlerMapping requestMappingHandlerMapping,
        RequestLoggingPathPolicy pathPolicy
    ) {
        this.paths = collectPaths(requestMappingHandlerMapping, pathPolicy);
    }

    public List<String> paths() {
        return paths;
    }

    private static List<String> collectPaths(
        RequestMappingHandlerMapping requestMappingHandlerMapping,
        RequestLoggingPathPolicy pathPolicy
    ) {
        Set<String> deduplicatedPaths = new TreeSet<>(Comparator.naturalOrder());
        for (RequestMappingInfo mappingInfo : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            for (String candidatePath : extractPatternValues(mappingInfo)) {
                if (!pathPolicy.shouldCaptureAtAll(candidatePath)) {
                    continue;
                }
                if (pathPolicy.shouldSkipSuccess(candidatePath)) {
                    continue;
                }
                if (pathPolicy.isProbablyStaticAsset(candidatePath, null)) {
                    continue;
                }
                deduplicatedPaths.add(candidatePath);
            }
        }
        return List.copyOf(deduplicatedPaths);
    }

    private static Set<String> extractPatternValues(RequestMappingInfo mappingInfo) {
        PathPatternsRequestCondition pathPatternsCondition = mappingInfo.getPathPatternsCondition();
        if (pathPatternsCondition != null) {
            return pathPatternsCondition.getPatternValues();
        }
        PatternsRequestCondition patternsCondition = mappingInfo.getPatternsCondition();
        if (patternsCondition == null) {
            return Set.of();
        }
        return Objects.requireNonNullElse(patternsCondition.getPatterns(), Set.of());
    }
}
