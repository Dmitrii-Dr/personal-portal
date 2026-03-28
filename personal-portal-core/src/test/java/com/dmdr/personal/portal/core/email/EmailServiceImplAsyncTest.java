package com.dmdr.personal.portal.core.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceImplAsyncTest {

    @Test
    void shouldRecordFailureWhenAsyncEmailSendThrows() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        Mockito.doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            mailSender,
            "from@test.local",
            "Portal",
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
    void shouldNotRecordFailureWhenAsyncEmailSendSucceeds() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            mailSender,
            "from@test.local",
            "Portal",
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
