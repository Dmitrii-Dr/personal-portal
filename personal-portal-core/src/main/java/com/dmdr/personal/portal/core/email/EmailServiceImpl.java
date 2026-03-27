package com.dmdr.personal.portal.core.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

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
 * Implementation of EmailService using Spring Mail.
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
        "booking-cancellation-user.html",
        "booking-cancellation-admin.html",
        "password-reset.html",
        "account-verification-code.html"
    };

    /**
     * Loaded at startup and reused for every email send (static per requirement).
     * Values are raw HTML templates (placeholders preserved).
     */
    private static final Object TEMPLATE_CACHE_LOCK = new Object();
    private static volatile Map<String, String> templateHtmlCache = new HashMap<>();
    private static volatile boolean templatesPreloaded = false;
    private static volatile String preloadedTemplatesDirectory = "";

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;
    private final EmailTemplateProperties emailTemplateProperties;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.from.email}") String fromEmail,
            @Value("${spring.mail.from.name}") String fromName,
            EmailTemplateProperties emailTemplateProperties) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.fromEmail = Objects.requireNonNull(fromEmail, "spring.mail.from.email");
        this.fromName = Objects.requireNonNull(fromName, "spring.mail.from.name");
        this.emailTemplateProperties = Objects.requireNonNull(emailTemplateProperties, "emailTemplateProperties");

        preloadTemplatesAtStartup();
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Ваш аккаунт готов к работе!");

            String htmlContent = buildWelcomeEmailHtml(firstName, lastName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            // Log error but don't throw - email sending failure shouldn't break user creation
            System.err.println("Failed to send welcome email to " + toEmail + ": " + e.getMessage());
        }
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
                                            String sessionTypeName, Instant startTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Confirmed - " + sessionTypeName);

            String htmlContent = buildBookingConfirmationEmailHtml(firstName, lastName, sessionTypeName, startTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking confirmation email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public void sendBookingRejectionEmail(String toEmail, String firstName, String lastName,
                                         String sessionTypeName, Instant startTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Declined - " + sessionTypeName);

            String htmlContent = buildBookingRejectionEmailHtml(firstName, lastName, sessionTypeName, startTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking rejection email to " + toEmail + ": " + e.getMessage());
        }
    }

    private String buildBookingConfirmationEmailHtml(String firstName, String lastName, String sessionTypeName, Instant startTime) {
        try {
            String template = loadTemplate("booking-confirmation.html");
            
            String displayName = firstName + " " + lastName;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedStartTime = formatter.format(startTime);
            
            return template
                    .replace("{{displayName}}", displayName)
                    .replace("{{sessionTypeName}}", sessionTypeName != null ? sessionTypeName : "your session")
                    .replace("{{startTime}}", formattedStartTime);
        } catch (IOException e) {
            System.err.println("Failed to load booking confirmation email template: " + e.getMessage());
            throw new RuntimeException("Failed to load booking confirmation email template: " + e);
        }
    }

    private String buildBookingRejectionEmailHtml(String firstName, String lastName, String sessionTypeName, Instant startTime) {
        try {
            String template = loadTemplate("booking-rejection.html");
            
            String displayName = firstName + " " + lastName;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedStartTime = formatter.format(startTime);
            
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
                                            String sessionTypeName, Instant startTime, String clientMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("New Booking Request - " + sessionTypeName);

            String htmlContent = buildBookingRequestAdminEmailHtml(clientName, clientEmail, sessionTypeName, startTime, clientMessage);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking request admin email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public void sendBookingRequestUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant startTime, String clientMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Request Received - " + sessionTypeName);

            String htmlContent = buildBookingRequestUserEmailHtml(
                    firstName,
                    lastName,
                    sessionTypeName,
                    startTime,
                    clientMessage);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking request user email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public void sendBookingUpdateRequestUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Update Request Received - " + sessionTypeName);

            String htmlContent = buildBookingUpdateRequestUserEmailHtml(
                    firstName,
                    lastName,
                    sessionTypeName,
                    oldStartTime,
                    newStartTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking update request user email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public void sendBookingCancellationUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant startTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Cancelled - " + (sessionTypeName != null ? sessionTypeName : "Session"));

            String htmlContent = buildBookingCancellationUserEmailHtml(firstName, lastName, sessionTypeName, startTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking cancellation user email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public void sendBookingCancellationAdminEmail(String toEmail, String clientName, String clientEmail,
            String sessionTypeName, Instant startTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Booking Cancelled - " + (sessionTypeName != null ? sessionTypeName : "Session"));

            String htmlContent = buildBookingCancellationAdminEmailHtml(clientName, clientEmail, sessionTypeName, startTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send booking cancellation admin email to " + toEmail + ": " + e.getMessage());
        }
    }

    private String buildBookingRequestAdminEmailHtml(String clientName, String clientEmail, String sessionTypeName, 
                                                    Instant startTime, String clientMessage) {
        try {
            String template = loadTemplate("booking-request-admin.html");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedStartTime = formatter.format(startTime);
            
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
            Instant startTime, String clientMessage) {
        try {
            String template = loadTemplate("booking-request-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedStartTime = formatter.format(startTime);

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
            Instant oldStartTime, Instant newStartTime) {
        try {
            String template = loadTemplate("booking-update-request-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));
 
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedOldStartTime = formatter.format(oldStartTime);
            String formattedNewStartTime = formatter.format(newStartTime);

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

    private String buildBookingCancellationUserEmailHtml(String firstName, String lastName, String sessionTypeName,
            Instant startTime) {
        try {
            String template = loadTemplate("booking-cancellation-user.html");

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedStartTime = formatter.format(startTime);

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
            Instant startTime) {
        try {
            String template = loadTemplate("booking-cancellation-admin.html");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", Locale.forLanguageTag("ru-RU"))
                    .withZone(ZoneId.systemDefault());
            String formattedStartTime = formatter.format(startTime);

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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password");

            String htmlContent = buildPasswordResetEmailHtml(firstName, lastName, resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send password reset email to " + toEmail + ": " + e.getMessage());
        }
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Verify your Personal Portal account");

            String htmlContent = buildAccountVerificationEmailHtml(
                    firstName,
                    lastName,
                    verificationCode,
                    expiryMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send account verification email to " + toEmail + ": " + e.getMessage());
        }
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
}
