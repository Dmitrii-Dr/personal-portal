package com.dmdr.personal.portal.admin.observability.service;

import com.dmdr.personal.portal.admin.observability.api.EndpointStatsDailyQuery;
import com.dmdr.personal.portal.admin.observability.api.EndpointStatsPeriodQuery;
import com.dmdr.personal.portal.admin.observability.api.EndpointStatsDailySpecifications;
import com.dmdr.personal.portal.admin.observability.api.EndpointTopErrorsQuery;
import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsPeriodResponse;
import com.dmdr.personal.portal.admin.observability.api.response.TopErrorEndpointResponse;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import com.dmdr.personal.portal.admin.observability.repository.EndpointRequestStatsDailyRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class DefaultEndpointStatsDailyQueryService implements EndpointStatsDailyQueryService {

    private final EndpointRequestStatsDailyRepository repository;

    public DefaultEndpointStatsDailyQueryService(EndpointRequestStatsDailyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<EndpointRequestStatsDailyEntity> search(EndpointStatsDailyQuery query) {
        return repository.findAll(EndpointStatsDailySpecifications.byQuery(query), Objects.requireNonNull(query.pageable()));
    }

    @Override
    public List<EndpointStatsPeriodResponse> searchPeriod(EndpointStatsPeriodQuery query) {
        boolean hasMethods = query.methods() != null && !query.methods().isEmpty();
        boolean hasTemplatePaths = query.templatePaths() != null && !query.templatePaths().isEmpty();
        if (hasMethods && hasTemplatePaths) {
            return repository.aggregatePeriodByMethodAndTemplatePath(query.from(), query.to(), query.methods(), query.templatePaths())
                .stream()
                .map(row -> new EndpointStatsPeriodResponse(
                    row.getMethod(),
                    row.getTemplatePath(),
                    row.getTotalCount(),
                    row.getSuccessCount(),
                    row.getAuthErrorCount(),
                    row.getClientErrorCount(),
                    row.getServerErrorCount(),
                    row.getOtherNonSuccessCount()
                ))
                .toList();
        }
        if (hasMethods) {
            return repository.aggregatePeriodByMethod(query.from(), query.to(), query.methods())
                .stream()
                .map(row -> new EndpointStatsPeriodResponse(
                    row.getMethod(),
                    null,
                    row.getTotalCount(),
                    row.getSuccessCount(),
                    row.getAuthErrorCount(),
                    row.getClientErrorCount(),
                    row.getServerErrorCount(),
                    row.getOtherNonSuccessCount()
                ))
                .toList();
        }
        if (hasTemplatePaths) {
            return repository.aggregatePeriodByTemplatePath(query.from(), query.to(), query.templatePaths())
                .stream()
                .map(row -> new EndpointStatsPeriodResponse(
                    null,
                    row.getTemplatePath(),
                    row.getTotalCount(),
                    row.getSuccessCount(),
                    row.getAuthErrorCount(),
                    row.getClientErrorCount(),
                    row.getServerErrorCount(),
                    row.getOtherNonSuccessCount()
                ))
                .toList();
        }
        return List.of();
    }

    @Override
    public Page<TopErrorEndpointResponse> searchTopErrors(EndpointTopErrorsQuery query) {
        return repository.findTopErrorEndpoints(query.from(), query.to(), Objects.requireNonNull(query.pageable()))
            .map(row -> new TopErrorEndpointResponse(
                row.getMethod(),
                row.getTemplatePath(),
                row.getTotalCount(),
                row.getSuccessCount(),
                row.getAuthErrorCount(),
                row.getClientErrorCount(),
                row.getServerErrorCount(),
                row.getOtherNonSuccessCount(),
                row.getTotalErrorCount()
            ));
    }
}
