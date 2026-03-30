package com.dmdr.personal.portal.core.email;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * HTML email via Spring {@link JavaMailSender} (dev Mailpit, test SMTP, or production SMTP).
 */
@Component
@ConditionalOnProperty(prefix = "app.email", name = "transport", havingValue = "smtp")
public class SmtpHtmlEmailDispatcher implements HtmlEmailDispatcher {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;

    public SmtpHtmlEmailDispatcher(
            JavaMailSender mailSender,
            @Value("${spring.mail.from.email}") String fromEmail,
            @Value("${spring.mail.from.name}") String fromName) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.fromEmail = Objects.requireNonNull(fromEmail, "spring.mail.from.email");
        this.fromName = Objects.requireNonNull(fromName, "spring.mail.from.name");
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }
}
