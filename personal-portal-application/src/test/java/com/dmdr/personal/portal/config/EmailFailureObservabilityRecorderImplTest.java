package com.dmdr.personal.portal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.dmdr.personal.portal.admin.observability.persistence.RequestLogPersistenceGateway;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogRecord;
import com.dmdr.personal.portal.core.email.EmailRequestContextSnapshot;
import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import com.dmdr.personal.portal.service.exception.PortalErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class EmailFailureObservabilityRecorderImplTest {

    @Test
    void shouldEnqueueDedicatedRequestLogRecordOnAsyncEmailFailure() {
        RequestLogPersistenceGateway gateway = Mockito.mock(RequestLogPersistenceGateway.class);
        EmailFailureObservabilityRecorderImpl recorder = new EmailFailureObservabilityRecorderImpl(gateway);
        EmailRequestContextSnapshot snapshot = new EmailRequestContextSnapshot("/api/v1/auth/registry", "POST", UUID.randomUUID());
        PersonalPortalRuntimeException portalException = new PersonalPortalRuntimeException(PortalErrorCode.EMAIL_SENDING_FAILED);
        RuntimeException cause = new RuntimeException("smtp timeout");

        recorder.recordFailure(snapshot, "to@test.local", "welcome", portalException, cause);

        ArgumentCaptor<RequestLogRecord> recordCaptor = ArgumentCaptor.forClass(RequestLogRecord.class);
        verify(gateway).enqueue(recordCaptor.capture());
        RequestLogRecord record = recordCaptor.getValue();
        assertThat(record.path()).isEqualTo("/api/v1/auth/registry");
        assertThat(record.method()).isEqualTo("POST");
        assertThat(record.status()).isEqualTo(500);
        assertThat(record.errorCode()).isEqualTo(PortalErrorCode.EMAIL_SENDING_FAILED.getCode());
        assertThat(record.errorMessage()).contains("welcome").contains("to@test.local");
        assertThat(record.stackTrace()).contains("smtp timeout");
    }
}
