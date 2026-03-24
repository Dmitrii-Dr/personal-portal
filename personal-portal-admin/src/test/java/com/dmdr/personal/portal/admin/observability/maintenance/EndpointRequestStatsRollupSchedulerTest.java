package com.dmdr.personal.portal.admin.observability.maintenance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class EndpointRequestStatsRollupSchedulerTest {

    @Test
    void rollUpSinceLastCheckpoint_shouldDelegateToService() {
        EndpointRequestStatsRollupService service = mock(EndpointRequestStatsRollupService.class);
        EndpointRequestStatsRollupScheduler scheduler = new EndpointRequestStatsRollupScheduler(service);

        scheduler.rollUpSinceLastCheckpoint();

        verify(service).rollUpSinceLastCheckpoint();
    }
}
