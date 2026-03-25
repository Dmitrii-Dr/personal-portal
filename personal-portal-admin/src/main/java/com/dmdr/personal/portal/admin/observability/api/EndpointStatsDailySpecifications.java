package com.dmdr.personal.portal.admin.observability.api;

import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EndpointStatsDailySpecifications {

    private EndpointStatsDailySpecifications() {
    }

    public static Specification<EndpointRequestStatsDailyEntity> byQuery(EndpointStatsDailyQuery query) {
        return fromInclusive(query.from())
            .and(toInclusive(query.to()))
            .and(methodsIn(query.methods()))
            .and(templatePathsIn(query.templatePaths()));
    }

    private static Specification<EndpointRequestStatsDailyEntity> fromInclusive(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("bucketStart"), from);
    }

    private static Specification<EndpointRequestStatsDailyEntity> toInclusive(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("bucketStart"), to);
    }

    private static Specification<EndpointRequestStatsDailyEntity> methodsIn(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        return (root, q, cb) -> cb.upper(root.get("method")).in(methods);
    }

    private static Specification<EndpointRequestStatsDailyEntity> templatePathsIn(List<String> templatePaths) {
        if (templatePaths == null || templatePaths.isEmpty()) {
            return null;
        }
        return (root, q, cb) -> root.get("templatePath").in(templatePaths);
    }
}
