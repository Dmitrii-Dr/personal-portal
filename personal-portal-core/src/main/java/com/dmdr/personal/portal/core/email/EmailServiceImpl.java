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
}

