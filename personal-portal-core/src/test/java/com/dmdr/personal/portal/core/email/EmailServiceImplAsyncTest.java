package com.dmdr.personal.portal.core.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;

class EmailServiceImplAsyncTest {

    @Test
    void shouldRecordFailureWhenAsyncEmailSendThrows() throws Exception {
        HtmlEmailDispatcher dispatcher = Mockito.mock(HtmlEmailDispatcher.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);

        doThrow(new RuntimeException("send failed")).when(dispatcher).sendHtml(anyString(), anyString(), anyString());

        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            dispatcher,
            templateProperties,
            new SyncTaskExecutor(),
            recorder
        );

        service.sendWelcomeEmail("to@test.local", "Test", "User");

        verify(recorder, times(1)).recordFailure(
            any(EmailRequestContextSnapshot.class),
            any(String.class),
            any(String.class),
            any(PersonalPortalRuntimeException.class),
            any(Throwable.class)
        );
    }

    @Test
    void shouldNotRecordFailureWhenAsyncEmailSendSucceeds() throws Exception {
        HtmlEmailDispatcher dispatcher = Mockito.mock(HtmlEmailDispatcher.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);

        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            dispatcher,
            templateProperties,
            new SyncTaskExecutor(),
            recorder
        );

        service.sendWelcomeEmail("to@test.local", "Test", "User");

        verify(recorder, never()).recordFailure(
            any(EmailRequestContextSnapshot.class),
            any(String.class),
            any(String.class),
            any(PersonalPortalRuntimeException.class),
            any(Throwable.class)
        );
    }
}
