package com.dmdr.personal.portal.core.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of EmailService using Spring Mail.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.from.email}") String fromEmail,
            @Value("${spring.mail.from.name}") String fromName) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
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
            ClassPathResource resource = new ClassPathResource("templates/email/welcome.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
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
            ClassPathResource resource = new ClassPathResource("templates/email/booking-confirmation.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            String displayName = firstName + " " + lastName;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)
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
            ClassPathResource resource = new ClassPathResource("templates/email/booking-rejection.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            String displayName = firstName + " " + lastName;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)
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

    private String buildBookingRequestAdminEmailHtml(String clientName, String clientEmail, String sessionTypeName, 
                                                    Instant startTime, String clientMessage) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/booking-request-admin.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)
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
            ClassPathResource resource = new ClassPathResource("templates/email/booking-request-user.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));
            if (displayName.isBlank()) {
                displayName = "друг";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)
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
            ClassPathResource resource = new ClassPathResource("templates/email/booking-update-request-user.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String displayName = Stream.of(firstName, lastName)
                    .filter(part -> part != null && !part.isBlank())
                    .collect(Collectors.joining(" "));
            if (displayName.isBlank()) {
                displayName = "друг";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)
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
            ClassPathResource resource = new ClassPathResource("templates/email/password-reset.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
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
            ClassPathResource resource = new ClassPathResource("templates/email/account-verification-code.html");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

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
