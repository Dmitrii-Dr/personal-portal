package com.dmdr.personal.portal.core.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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

    @Test
    void shouldRenderBookingConfirmationTimeInProvidedRecipientZone() throws Exception {
        HtmlEmailDispatcher dispatcher = Mockito.mock(HtmlEmailDispatcher.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);
        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            dispatcher,
            templateProperties,
            new SyncTaskExecutor(),
            recorder
        );
        Instant startTime = Instant.parse("2026-01-15T12:00:00Z");
        ZoneId recipientZone = ZoneId.of("GMT+03:00");
        String expectedTime = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
            .withZone(recipientZone)
            .format(startTime);

        service.sendBookingConfirmationEmail(
            "to@test.local",
            "Test",
            "User",
            "Session",
            startTime,
            recipientZone
        );

        verify(dispatcher, times(1)).sendHtml(eq("to@test.local"), anyString(), org.mockito.ArgumentMatchers.contains(expectedTime));
    }

    @Test
    void shouldRenderServiceStartedTimeInProvidedRecipientZone() throws Exception {
        HtmlEmailDispatcher dispatcher = Mockito.mock(HtmlEmailDispatcher.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);
        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            dispatcher,
            templateProperties,
            new SyncTaskExecutor(),
            recorder
        );
        Instant startedAt = Instant.parse("2026-06-01T00:00:00Z");
        ZoneId recipientZone = ZoneId.of("GMT+10:00");
        String expectedTime = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
            .withZone(recipientZone)
            .format(startedAt);

        service.sendAdminServiceStartedEmail("to@test.local", startedAt, recipientZone);

        verify(dispatcher, times(1)).sendHtml(eq("to@test.local"), anyString(), org.mockito.ArgumentMatchers.contains(expectedTime));
    }

    @Test
    void shouldRenderBookingUpdateRequestAdminTimesInProvidedRecipientZone() throws Exception {
        HtmlEmailDispatcher dispatcher = Mockito.mock(HtmlEmailDispatcher.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);
        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            dispatcher,
            templateProperties,
            new SyncTaskExecutor(),
            recorder
        );
        Instant oldStartTime = Instant.parse("2026-01-15T12:00:00Z");
        Instant newStartTime = Instant.parse("2026-01-16T13:30:00Z");
        ZoneId recipientZone = ZoneId.of("GMT+03:00");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
            .withZone(recipientZone);
        String expectedOldTime = formatter.format(oldStartTime);
        String expectedNewTime = formatter.format(newStartTime);

        service.sendBookingUpdateRequestAdminEmail(
            "admin@test.local",
            "Client User",
            "client@test.local",
            "Session",
            oldStartTime,
            newStartTime,
            recipientZone
        );

        verify(dispatcher, times(1)).sendHtml(
            eq("admin@test.local"),
            anyString(),
            org.mockito.ArgumentMatchers.contains(expectedOldTime)
        );
        verify(dispatcher, times(1)).sendHtml(
            eq("admin@test.local"),
            anyString(),
            org.mockito.ArgumentMatchers.contains(expectedNewTime)
        );
    }

    @Test
    void shouldRenderBookingUpdatedByAdminUserTimesInProvidedRecipientZone() throws Exception {
        HtmlEmailDispatcher dispatcher = Mockito.mock(HtmlEmailDispatcher.class);
        EmailFailureObservabilityRecorder recorder = Mockito.mock(EmailFailureObservabilityRecorder.class);
        EmailTemplateProperties templateProperties = new EmailTemplateProperties();
        EmailServiceImpl service = new EmailServiceImpl(
            dispatcher,
            templateProperties,
            new SyncTaskExecutor(),
            recorder
        );
        Instant oldStartTime = Instant.parse("2026-02-10T08:00:00Z");
        Instant newStartTime = Instant.parse("2026-02-11T10:15:00Z");
        ZoneId recipientZone = ZoneId.of("GMT+04:00");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
            .withZone(recipientZone);
        String expectedOldTime = formatter.format(oldStartTime);
        String expectedNewTime = formatter.format(newStartTime);

        service.sendBookingUpdatedByAdminUserEmail(
            "user@test.local",
            "Test",
            "User",
            "Session",
            oldStartTime,
            newStartTime,
            recipientZone
        );

        verify(dispatcher, times(1)).sendHtml(
            eq("user@test.local"),
            anyString(),
            org.mockito.ArgumentMatchers.contains(expectedOldTime)
        );
        verify(dispatcher, times(1)).sendHtml(
            eq("user@test.local"),
            anyString(),
            org.mockito.ArgumentMatchers.contains(expectedNewTime)
        );
    }
}
