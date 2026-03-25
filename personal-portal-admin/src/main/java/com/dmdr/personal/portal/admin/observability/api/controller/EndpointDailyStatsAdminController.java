package com.dmdr.personal.portal.admin.observability.api.controller;

import java.time.LocalDate;
import java.util.List;

import com.dmdr.personal.portal.admin.observability.api.EndpointStatsDailyQuery;
import com.dmdr.personal.portal.admin.observability.api.EndpointStatsPeriodQuery;
import com.dmdr.personal.portal.admin.observability.api.EndpointTopErrorsQuery;
import com.dmdr.personal.portal.admin.observability.service.EndpointStatsDailyQueryService;
import com.dmdr.personal.portal.admin.observability.api.mapper.EndpointStatsDailyMapper;
import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsDailyResponse;
import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsPeriodResponse;
import com.dmdr.personal.portal.admin.observability.api.response.TopErrorEndpointResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/observability/endpoint-stats")
public class EndpointDailyStatsAdminController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TOP_ERRORS_PAGE_SIZE = 20;
    private static final int MAX_RANGE_MONTHS = 1;

    private final EndpointStatsDailyQueryService queryService;
    private final EndpointStatsDailyMapper mapper;

    public EndpointDailyStatsAdminController(EndpointStatsDailyQueryService queryService, EndpointStatsDailyMapper mapper) {
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<EndpointStatsDailyResponse> list(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to,
        @RequestParam List<String> methods,
        @RequestParam List<String> templatePaths,
        @PageableDefault(size = 20, sort = {"bucketStart", "method", "templatePath"}, direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before or equal to 'to'");
        }
        List<String> normalizedMethods = normalizedMethods(methods);
        List<String> normalizedTemplatePaths = normalizedTemplatePaths(templatePaths);
        validateNonEmptyListFilter("methods", normalizedMethods);
        validateNonEmptyListFilter("templatePaths", normalizedTemplatePaths);
        EndpointStatsDailyQuery query = new EndpointStatsDailyQuery(
            from,
            to,
            normalizedMethods,
            normalizedTemplatePaths,
            withMaxPageSize(pageable, MAX_PAGE_SIZE)
        );
        return queryService.search(query).map(mapper::toResponse);
    }

    @GetMapping("/period")
    public List<EndpointStatsPeriodResponse> period(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to,
        @RequestParam List<String> methods,
        @RequestParam List<String> templatePaths
    ) {
        validatePeriodRange(from, to);
        List<String> normalizedMethods = normalizedMethods(methods);
        List<String> normalizedTemplatePaths = normalizedTemplatePaths(templatePaths);
        validateNonEmptyListFilter("methods", normalizedMethods);
        validateNonEmptyListFilter("templatePaths", normalizedTemplatePaths);
        EndpointStatsPeriodQuery query = new EndpointStatsPeriodQuery(
            from,
            to,
            normalizedMethods,
            normalizedTemplatePaths
        );
        return queryService.searchPeriod(query);
    }

    @GetMapping("/period/top-errors")
    public Page<TopErrorEndpointResponse> topErrors(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        validatePeriodRange(from, to);
        EndpointTopErrorsQuery query = new EndpointTopErrorsQuery(
            from,
            to,
            withMaxPageSize(pageable, MAX_TOP_ERRORS_PAGE_SIZE)
        );
        return queryService.searchTopErrors(query);
    }

    private static void validatePeriodRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before or equal to 'to'");
        }
        if (to.isAfter(from.plusMonths(MAX_RANGE_MONTHS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested period exceeds one calendar month");
        }
    }

    private static List<String> normalizedMethods(List<String> methods) {
        if (methods == null) {
            return null;
        }
        List<String> normalized = methods.stream()
            .map(value -> value == null ? "" : value.trim())
            .filter(value -> !value.isBlank())
            .map(String::toUpperCase)
            .distinct()
            .toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> normalizedTemplatePaths(List<String> templatePaths) {
        if (templatePaths == null) {
            return null;
        }
        List<String> normalized = templatePaths.stream()
            .map(value -> value == null ? "" : value.trim())
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Pageable withMaxPageSize(Pageable pageable, int maxPageSize) {
        int boundedSize = Math.min(pageable.getPageSize(), maxPageSize);
        if (boundedSize == pageable.getPageSize()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), boundedSize, pageable.getSort());
    }

    private static void validateNonEmptyListFilter(String paramName, List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'" + paramName + "' must not be empty");
        }
    }
}
