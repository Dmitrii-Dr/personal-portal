package com.dmdr.personal.portal.core.email;

import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * HTML email via Yandex Cloud Postbox using the Amazon SES API v2 ({@link SesV2Client#sendEmail}).
 *
 * <p>Configuration: {@code docs/deploy-guide.md} (Postbox / email).
 *
 * @see <a href="https://yandex.cloud/en/docs/postbox/">Yandex Cloud Postbox</a>
 */
@Component
@ConditionalOnProperty(prefix = "app.email", name = "transport", havingValue = "postbox")
public class PostboxHtmlEmailDispatcher implements HtmlEmailDispatcher {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    private final SesV2Client sesClient;
    private final String fromEmail;
    private final String fromName;

    public PostboxHtmlEmailDispatcher(
            SesV2Client postboxSesClient,
            @Value("${spring.mail.from.email}") String fromEmail,
            @Value("${spring.mail.from.name}") String fromName) {
        this.sesClient = Objects.requireNonNull(postboxSesClient, "postboxSesClient");
        this.fromEmail = Objects.requireNonNull(fromEmail, "spring.mail.from.email");
        this.fromName = Objects.requireNonNull(fromName, "spring.mail.from.name");
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(formatSource())
                .destination(Destination.builder().toAddresses(List.of(to)).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset(UTF_8).build())
                                .body(Body.builder()
                                        .html(Content.builder().data(htmlBody).charset(UTF_8).build())
                                        .build())
                                .build())
                        .build())
                .build();
        sesClient.sendEmail(request);
    }

    private String formatSource() {
        try {
            return new InternetAddress(fromEmail, fromName, UTF_8).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid spring.mail.from.email / spring.mail.from.name for Postbox", e);
        }
    }
}
