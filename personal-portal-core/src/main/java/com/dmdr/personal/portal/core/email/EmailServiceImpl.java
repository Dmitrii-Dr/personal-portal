package com.dmdr.personal.portal.core.email;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import com.dmdr.personal.portal.service.exception.PortalErrorCode;

import java.io.IOException;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link EmailService} implementation: builds HTML from templates and sends via {@link HtmlEmailDispatcher}
 * (SMTP or Yandex Cloud Postbox / SES API v2 depending on {@code app.email.transport}).
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String[] TEMPLATE_NAMES = new String[] {
        "welcome.html",
        "booking-confirmation.html",
        "booking-rejection.html",
        "booking-request-admin.html",
        "booking-request-user.html",
        "booking-update-request-user.html",
        "booking-update-request-admin.html",
        "booking-updated-by-admin-user.html",
        "booking-cancellation-user.html",
        "booking-cancellation-admin.html",
        "password-reset.html",
        "account-verification-code.html",
        "service-started-admin.html"
    };

    /**
     * Loaded at startup and reused for every email send (static per requirement).
     * Values are raw HTML templates (placeholders preserved).
     */
    private static final Object TEMPLATE_CACHE_LOCK = new Object();
    private static volatile Map<String, String> templateHtmlCache = new HashMap<>();
    private static volatile boolean templatesPreloaded = false;
    private static volatile String preloadedTemplatesDirectory = "";
    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"));

    private final HtmlEmailDispatcher htmlEmailDispatcher;
    private final EmailTemplateProperties emailTemplateProperties;
    private final TaskExecutor emailTaskExecutor;
    private final EmailFailureObservabilityRecorder emailFailureObservabilityRecorder;

    public EmailServiceImpl(
            HtmlEmailDispatcher htmlEmailDispatcher,
            EmailTemplateProperties emailTemplateProperties,
            @Qualifier("emailTaskExecutor") TaskExecutor emailTaskExecutor,
            EmailFailureObservabilityRecorder emailFailureObservabilityRecorder) {
        this.htmlEmailDispatcher = Objects.requireNonNull(htmlEmailDispatcher, "htmlEmailDispatcher");
        this.emailTemplateProperties = Objects.requireNonNull(emailTemplateProperties, "emailTemplateProperties");
        this.emailTaskExecutor = Objects.requireNonNull(emailTaskExecutor, "emailTaskExecutor");
        this.emailFailureObservabilityRecorder = Objects.requireNonNull(emailFailureObservabilityRecorder, "emailFailureObservabilityRecorder");

        preloadTemplatesAtStartup();
    }

    private void sendAsync(String emailType, String recipient, EmailSendAction action) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        EmailRequestContextSnapshot contextSnapshot = EmailRequestContextSnapshot.captureCurrent();
        emailTaskExecutor.execute(() -> {
            try {
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                }
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                } else {
                    MDC.clear();
                }
                action.run();
            } catch (Exception e) {
                PersonalPortalRuntimeException portalException =
                        new PersonalPortalRuntimeException(PortalErrorCode.EMAIL_SENDING_FAILED);
                log.error(
                        "Email send failed: type={}, recipient={}, code={}, message={}",
                        emailType,
                        recipient,
                        portalException.getErrorCode().getCode(),
                        e.getMessage(),
                        e
                );
                emailFailureObservabilityRecorder.recordFailure(contextSnapshot, recipient, emailType, portalException, e);
            } finally {
                RequestContextHolder.resetRequestAttributes();
                MDC.clear();
            }
        });
    }

    private interface EmailSendAction {
        void run() throws Exception;
    }

    private String loadTemplate(String templateName) throws IOException {
        Objects.requireNonNull(templateName, "templateName");
        String template = templateHtmlCache.get(templateName);
        if (template == null) {
            throw new RuntimeException("Email template not preloaded: " + templateName);
        }
        return template;
    }

    /**
     * Preloads templates once at startup (OS directory override first; classpath fallback).
     * After this runs, {@link #loadTemplate(String)} does not perform any filesystem IO.
     */
    private void preloadTemplatesAtStartup() {
        log.info("preloading mail templates");
        String normalizedDirectory = StringUtils.hasText(emailTemplateProperties.getDirectory())
            ? emailTemplateProperties.getDirectory()
            : "";
        log.info("Mail template directory: {}", normalizedDirectory);
        synchronized (TEMPLATE_CACHE_LOCK) {
            if (templatesPreloaded && Objects.equals(preloadedTemplatesDirectory, normalizedDirectory)) {
                log.info("Mail template already preloaded from: {}", preloadedTemplatesDirectory);
                return;
            }
            log.info("Preloading mail templates from: {}", normalizedDirectory);
            Map<String, String> cache = new HashMap<>();
            for (String templateName : TEMPLATE_NAMES) {
                cache.put(templateName, resolveTemplateHtml(templateName));
            }
            templateHtmlCache = Map.copyOf(cache);
            templatesPreloaded = true;
            preloadedTemplatesDirectory = normalizedDirectory;
        }
    }

    private String resolveTemplateHtml(String templateName) {
        String directory = emailTemplateProperties.getDirectory();
        if (StringUtils.hasText(directory)) {
            Path templatePath = Path.of(directory).resolve(templateName);
            FileSystemResource fileSystemResource = new FileSystemResource(templatePath.toFile());
            if (fileSystemResource.exists() && fileSystemResource.isReadable()) {
                try {
                    return StreamUtils.copyToString(fileSystemResource.getInputStream(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load custom email template: " + templatePath, e);
                }
            }
            log.error("Custom email template not found: {}", templatePath);
        }
        log.info("Loading default email template from classpath: {}", templateName);
        ClassPathResource classPathResource = new ClassPathResource("templates/email/" + templateName);
        try {
            return StreamUtils.copyToString(classPathResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load default email template from classpath: " + templateName, e);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String firstName, String lastName) {
        sendAsync("welcome", toEmail, () -> {
            String htmlContent = buildWelcomeEmailHtml(firstName, lastName);
            htmlEmailDispatcher.sendHtml(toEmail, "Ваш аккаунт готов к работе!", htmlContent);
        });
    }

    private String buildWelcomeEmailHtml(String firstName, String lastName) {
        try {
            String template = loadTemplate("welcome.html");
            
            String displayName = firstName +  " " + lastName;
            
            return template.replace("{{displayName}}", displayName);
        } catch (IOException e) {
            System.err.println("Failed to load welcome email template: " + e.getMessage());
            throw new RuntimeException("Failed to load welcome email template:  " + e);

        }
    }

    @Override
    public void sendBookingConfirmationEmail(String toEmail, String firstName, String lastName,
                                            String sessionTypeName, Instant startTime, ZoneId recipientZoneId) {
        sendAsync("booking-confirmation", toEmail, () -> {
            String htmlContent = buildBookingConfirmationEmailHtml(firstName, lastName, sessionTypeName, startTime, recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Запись подтверждена", htmlContent);
        });
    }

    @Override
    public void sendBookingRejectionEmail(String toEmail, String firstName, String lastName,
                                         String sessionTypeName, Instant startTime, ZoneId recipientZoneId) {
        sendAsync("booking-rejection", toEmail, () -> {
            String htmlContent = buildBookingRejectionEmailHtml(firstName, lastName, sessionTypeName, startTime, recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Запрос на запись отклонен", htmlContent);
        });
    }

    private String buildBookingConfirmationEmailHtml(String firstName, String lastName, String sessionTypeName, Instant startTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-confirmation.html");
            
            String displayName = firstName + " " + lastName;
            String formattedStartTime = formatDateTime(startTime, recipientZoneId);
            
            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{startTime}}", formattedStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking confirmation email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking confirmation email template: " + e);
        }
    }

    private String buildBookingRejectionEmailHtml(String firstName, String lastName, String sessionTypeName, Instant startTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-rejection.html");
            
            String displayName = firstName + " " + lastName;
            String formattedStartTime = formatDateTime(startTime, recipientZoneId);
            
            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{startTime}}", formattedStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking rejection email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking rejection email template: " + e);
        }
    }

    @Override
    public void sendBookingRequestAdminEmail(String toEmail, String clientName, String clientEmail,
                                            String sessionTypeName, Instant startTime, String clientMessage, ZoneId recipientZoneId) {
        sendAsync("booking-request-admin", toEmail, () -> {
            String htmlContent = buildBookingRequestAdminEmailHtml(clientName, clientEmail, sessionTypeName, startTime, clientMessage, recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Новая запись", htmlContent);
        });
    }

    @Override
    public void sendBookingRequestUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant startTime, String clientMessage, ZoneId recipientZoneId) {
        sendAsync("booking-request-user", toEmail, () -> {
            String htmlContent = buildBookingRequestUserEmailHtml(
                    firstName,
                    lastName,
                    sessionTypeName,
                    startTime,
                    clientMessage,
                    recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Запрос на запись получен", htmlContent);
        });
    }

    @Override
    public void sendBookingUpdateRequestUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId) {
        sendAsync("booking-update-request-user", toEmail, () -> {
            String htmlContent = buildBookingUpdateRequestUserEmailHtml(
                    firstName,
                    lastName,
                    sessionTypeName,
                    oldStartTime,
                    newStartTime,
                    recipientZoneId);
            htmlEmailDispatcher.sendHtml(
                    toEmail,
                    "Изменение времени сессии",
                    htmlContent);
        });
    }

    @Override
    public void sendBookingUpdateRequestAdminEmail(String toEmail, String clientName, String clientEmail,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId) {
        sendAsync("booking-update-request-admin", toEmail, () -> {
            String htmlContent = buildBookingUpdateRequestAdminEmailHtml(
                    clientName,
                    clientEmail,
                    sessionTypeName,
                    oldStartTime,
                    newStartTime,
                    recipientZoneId);
            htmlEmailDispatcher.sendHtml(
                    toEmail,
                    "Изменение времени сессии",
                    htmlContent);
        });
    }

    @Override
    public void sendBookingUpdatedByAdminUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId) {
        sendAsync("booking-updated-by-admin-user", toEmail, () -> {
            String htmlContent = buildBookingUpdatedByAdminUserEmailHtml(
                    firstName,
                    lastName,
                    sessionTypeName,
                    oldStartTime,
                    newStartTime,
                    recipientZoneId);
            htmlEmailDispatcher.sendHtml(
                    toEmail,
                    "Время сессии изменено",
                    htmlContent);
        });
    }

    @Override
    public void sendBookingCancellationUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant startTime, ZoneId recipientZoneId) {
        sendAsync("booking-cancellation-user", toEmail, () -> {
            String htmlContent = buildBookingCancellationUserEmailHtml(firstName, lastName, sessionTypeName, startTime, recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Запись отменена", htmlContent);
        });
    }

    @Override
    public void sendBookingCancellationAdminEmail(String toEmail, String clientName, String clientEmail,
            String sessionTypeName, Instant startTime, ZoneId recipientZoneId) {
        sendAsync("booking-cancellation-admin", toEmail, () -> {
            String htmlContent = buildBookingCancellationAdminEmailHtml(clientName, clientEmail, sessionTypeName, startTime, recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Клиент отменил запись", htmlContent);
        });
    }

    private String buildBookingRequestAdminEmailHtml(String clientName, String clientEmail, String sessionTypeName, 
                                                    Instant startTime, String clientMessage, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-request-admin.html");
            
            String formattedStartTime = formatDateTime(startTime, recipientZoneId);
            
            String result = template
                    .replace("{{clientName}}", clientName != null ? clientName : "Unknown")
                    .replace("{{clientEmail}}", clientEmail != null ? clientEmail : "")
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "Unknown Session")
                    .replace("{{startTime}}", formattedStartTime);
            
            // Handle optional client message
            if (clientMessage != null && !clientMessage.trim().isEmpty()) {
                result = result.replace("{{clientMessage}}", clientMessage)
                        .replace("display: none;", "display: block;");
            } else {
                // Remove the client message section if no message
                result = result.replaceAll("(?s)<p id=\"client-message-section\".*?</p>", "");
            }
            
            return result;
        } catch (IOException e) {
            System.err.println("Failed to load booking request admin email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking request admin email template: " + e);
        }
    }

    private String buildBookingRequestUserEmailHtml(String firstName, String lastName, String sessionTypeName,
            Instant startTime, String clientMessage, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-request-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));

            String formattedStartTime = formatDateTime(startTime, recipientZoneId);

            String result = template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{startTime}}", formattedStartTime);

            if (clientMessage != null && !clientMessage.trim().isEmpty()) {
                result = result
                        .replace("{{clientMessage}}", clientMessage);
            } else {
                result = result
                        .replaceAll("(?s)<div id=\"client-message-section\".*?</div>", "");
            }

            return result;
        } catch (IOException e) {
            System.err.println("Failed to load booking request user email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking request user email template: " + e);
        }
    }

    private String buildBookingUpdateRequestUserEmailHtml(String firstName, String lastName, String sessionTypeName,
            Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-update-request-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));
 
            String formattedOldStartTime = formatDateTime(oldStartTime, recipientZoneId);
            String formattedNewStartTime = formatDateTime(newStartTime, recipientZoneId);

            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{oldStartTime}}", formattedOldStartTime)
                    .replace("{{newStartTime}}", formattedNewStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking update request user email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking update request user email template: " + e);
        }
    }

    private String buildBookingUpdateRequestAdminEmailHtml(String clientName, String clientEmail, String sessionTypeName,
            Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-update-request-admin.html");

            String formattedOldStartTime = formatDateTime(oldStartTime, recipientZoneId);
            String formattedNewStartTime = formatDateTime(newStartTime, recipientZoneId);

            return template
                    .replace("{{clientName}}", clientName != null ? clientName : "Unknown")
                    .replace("{{clientEmail}}", clientEmail != null ? clientEmail : "")
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "Unknown Session")
                    .replace("{{oldStartTime}}", formattedOldStartTime)
                    .replace("{{newStartTime}}", formattedNewStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking update request admin email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking update request admin email template: " + e);
        }
    }

    private String buildBookingUpdatedByAdminUserEmailHtml(String firstName, String lastName, String sessionTypeName,
            Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-updated-by-admin-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));

            String formattedOldStartTime = formatDateTime(oldStartTime, recipientZoneId);
            String formattedNewStartTime = formatDateTime(newStartTime, recipientZoneId);

            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{oldStartTime}}", formattedOldStartTime)
                    .replace("{{newStartTime}}", formattedNewStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking updated by admin user email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking updated by admin user email template: " + e);
        }
    }

    private String buildBookingCancellationUserEmailHtml(String firstName, String lastName, String sessionTypeName,
            Instant startTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-cancellation-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));

            String formattedStartTime = formatDateTime(startTime, recipientZoneId);

            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{startTime}}", formattedStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking cancellation user email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking cancellation user email template: " + e);
        }
    }

    private String buildBookingCancellationAdminEmailHtml(String clientName, String clientEmail, String sessionTypeName,
            Instant startTime, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("booking-cancellation-admin.html");

            String formattedStartTime = formatDateTime(startTime, recipientZoneId);

            return template
                    .replace("{{clientName}}", clientName != null ? clientName : "Unknown")
                    .replace("{{clientEmail}}", clientEmail != null ? clientEmail : "")
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "Unknown Session")
                    .replace("{{startTime}}", formattedStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking cancellation admin email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking cancellation admin email template: " + e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String firstName, String lastName, String resetLink) {
        sendAsync("password-reset", toEmail, () -> {
            String htmlContent = buildPasswordResetEmailHtml(firstName, lastName, resetLink);
            htmlEmailDispatcher.sendHtml(toEmail, "Сброс пароля", htmlContent);
        });
    }

    private String buildPasswordResetEmailHtml(String firstName, String lastName, String resetLink) {
        try {
            String template = loadTemplate("password-reset.html");
            
            String displayName = firstName + " " + lastName;
            
            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{resetLink}}", resetLink);
        } catch (IOException e) {
            System.err.println("Failed to load password reset email template: " + e.getMessage());
            throw new RuntimeException("Failed to load password reset email template: " + e);
        }
    }

    @Override
    public void sendAccountVerificationCodeEmail(String toEmail, String firstName, String lastName,
            String verificationCode, int expiryMinutes) {
        sendAsync("account-verification-code", toEmail, () -> {
            String htmlContent = buildAccountVerificationEmailHtml(
                    firstName,
                    lastName,
                    verificationCode,
                    expiryMinutes);
            htmlEmailDispatcher.sendHtml(toEmail, "Подтверждение регистрации", htmlContent);
        });
    }

    private String buildAccountVerificationEmailHtml(String firstName, String lastName, String verificationCode,
            int expiryMinutes) {
        try {
            String template = loadTemplate("account-verification-code.html");

            String displayName = firstName + " " + lastName;
            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{verificationCode}}", verificationCode)
                    .replace("{{expiryMinutes}}", String.valueOf(expiryMinutes));
        } catch (IOException e) {
            System.err.println("Failed to load account verification email template: " + e.getMessage());
            throw new RuntimeException("Failed to load account verification email template: " + e);
        }
    }

    @Override
    public void sendAdminServiceStartedEmail(String toEmail, Instant startedAt, ZoneId recipientZoneId) {
        sendAsync("service-started-admin", toEmail, () -> {
            String htmlContent = buildAdminServiceStartedEmailHtml(startedAt, recipientZoneId);
            htmlEmailDispatcher.sendHtml(toEmail, "Сайт запущен", htmlContent);
        });
    }

    private String buildAdminServiceStartedEmailHtml(Instant startedAt, ZoneId recipientZoneId) {
        try {
            String template = loadTemplate("service-started-admin.html");
            String formatted = formatDateTime(startedAt, recipientZoneId);
            return template.replace("{{startDateTime}}", formatted);
        } catch (IOException e) {
            System.err.println("Failed to load service started admin email template: " + e.getMessage());
            throw new RuntimeException("Failed to load service started admin email template: " + e);
        }
    }

    private String formatDateTime(Instant time, ZoneId recipientZoneId) {
        return EMAIL_DATE_TIME_FORMATTER.withZone(recipientZoneId).format(time);
    }
}
