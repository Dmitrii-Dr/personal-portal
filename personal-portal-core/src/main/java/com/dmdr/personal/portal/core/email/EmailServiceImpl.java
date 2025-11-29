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
            helper.setSubject("Welcome to Personal Portal!");

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
}

