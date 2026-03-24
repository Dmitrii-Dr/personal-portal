package com.dmdr.personal.portal.admin.observability.maintenance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class RequestLogRetentionSchedulerTest {

    @Test
    void purgeExpiredDetailRows_shouldDelegateToService() {
        RequestLogRetentionService service = mock(RequestLogRetentionService.class);
        RequestLogRetentionScheduler scheduler = new RequestLogRetentionScheduler(service);

        scheduler.purgeExpiredDetailRows();

        verify(service).purgeExpiredDetailRows();
    }
}
