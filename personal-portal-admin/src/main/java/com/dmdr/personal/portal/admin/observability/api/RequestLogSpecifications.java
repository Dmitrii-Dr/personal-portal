package com.dmdr.personal.portal.admin.observability.api;

import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Phase E filtering specs from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
public final class RequestLogSpecifications {

    private RequestLogSpecifications() {
    }

    public static Specification<RequestLogEntity> byQuery(RequestLogQuery query) {
        return fromInclusive(query.from())
            .and(toExclusive(query.to()))
            .and(statusEquals(query.status()))
            .and(templatePathEquals(query.templatePath()))
            .and(methodEquals(query.method()))
            .and(userIdEquals(query.userId()))
            .and(errorCodeContains(query.errorCodeContains()))
            .and(errorMessageContains(query.errorMessageContains()));
    }

    private static Specification<RequestLogEntity> fromInclusive(java.time.Instant from) {
        if (from == null) {
            return null;
        }
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private static Specification<RequestLogEntity> toExclusive(java.time.Instant to) {
        if (to == null) {
            return null;
        }
        return (root, q, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    private static Specification<RequestLogEntity> statusEquals(Integer status) {
        if (status == null) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<RequestLogEntity> templatePathEquals(String templatePath) {
        if (isBlank(templatePath)) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("templatePath"), templatePath.trim());
    }

    private static Specification<RequestLogEntity> methodEquals(String method) {
        if (isBlank(method)) {
            return null;
        }
        return (root, q, cb) -> cb.equal(cb.upper(root.get("method")), method.trim().toUpperCase());
    }

    private static Specification<RequestLogEntity> userIdEquals(java.util.UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("userId"), userId);
    }

    private static Specification<RequestLogEntity> errorCodeContains(String text) {
        if (isBlank(text)) {
            return null;
        }
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("errorCode")), like);
    }

    private static Specification<RequestLogEntity> errorMessageContains(String text) {
        if (isBlank(text)) {
            return null;
        }
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("errorMessage")), like);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
