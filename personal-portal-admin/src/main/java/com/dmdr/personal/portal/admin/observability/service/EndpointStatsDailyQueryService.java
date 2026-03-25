package com.dmdr.personal.portal.admin.observability.service;

import com.dmdr.personal.portal.admin.observability.api.EndpointStatsDailyQuery;
import com.dmdr.personal.portal.admin.observability.api.EndpointStatsPeriodQuery;
import com.dmdr.personal.portal.admin.observability.api.EndpointTopErrorsQuery;
import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import com.dmdr.personal.portal.admin.observability.api.response.EndpointStatsPeriodResponse;
import com.dmdr.personal.portal.admin.observability.api.response.TopErrorEndpointResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface EndpointStatsDailyQueryService {

    Page<EndpointRequestStatsDailyEntity> search(EndpointStatsDailyQuery query);

    List<EndpointStatsPeriodResponse> searchPeriod(EndpointStatsPeriodQuery query);

    Page<TopErrorEndpointResponse> searchTopErrors(EndpointTopErrorsQuery query);
}
