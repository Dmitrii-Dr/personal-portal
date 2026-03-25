package com.dmdr.personal.portal.admin.observability.api.controller;

import com.dmdr.personal.portal.admin.observability.api.RequestLogQuery;
import com.dmdr.personal.portal.admin.observability.service.RequestLogQueryService;
import com.dmdr.personal.portal.admin.observability.api.mapper.RequestLogAdminMapper;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogDetailResponse;
import com.dmdr.personal.portal.admin.observability.api.response.RequestLogListItemResponse;
import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phase E controller from docs/observability/dev/rest-request-observability-impl-plan-composer.md.
 */
@RestController
@RequestMapping("/api/v1/admin/observability/request-logs")
public class RequestLogAdminController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_RANGE_DAYS = 31;

    private final RequestLogQueryService requestLogQueryService;
    private final RequestLogAdminMapper requestLogAdminMapper;

    public RequestLogAdminController(
        RequestLogQueryService requestLogQueryService,
        RequestLogAdminMapper requestLogAdminMapper
    ) {
        this.requestLogQueryService = requestLogQueryService;
        this.requestLogAdminMapper = requestLogAdminMapper;
    }

    @GetMapping
    public Page<RequestLogListItemResponse> list(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String templatePath,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) String errorCodeContains,
        @RequestParam(required = false) String errorMessageContains,
        @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        validateRange(from, to);

        Pageable boundedPageable = withMaxPageSize(pageable, MAX_PAGE_SIZE);
        RequestLogQuery query = new RequestLogQuery(
            from,
            to,
            status,
            templatePath,
            method,
            userId,
            errorCodeContains,
            errorMessageContains,
            boundedPageable
        );
        return requestLogQueryService.search(query).map(requestLogAdminMapper::toListItem);
    }

    @GetMapping("/{id}")
    public RequestLogDetailResponse detail(@PathVariable long id) {
        RequestLogEntity entity = requestLogQueryService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request log not found"));
        return requestLogAdminMapper.toDetail(entity);
    }

    private void validateRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be earlier than 'to'");
        }
        if (Duration.between(from, to).toDays() > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Requested time range exceeds " + MAX_RANGE_DAYS + " days"
            );
        }
    }

    private static Pageable withMaxPageSize(Pageable pageable, int maxPageSize) {
        int boundedSize = Math.min(pageable.getPageSize(), maxPageSize);
        if (boundedSize == pageable.getPageSize()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), boundedSize, pageable.getSort());
    }
}
