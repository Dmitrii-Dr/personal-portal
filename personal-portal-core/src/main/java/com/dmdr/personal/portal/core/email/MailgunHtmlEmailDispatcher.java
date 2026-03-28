package com.dmdr.personal.portal.core.email;

import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * HTML email via Mailgun Messages API ({@code POST /v3/{domain}/messages}).
 */
@Component
@ConditionalOnProperty(prefix = "app.email", name = "transport", havingValue = "mailgun")
public class MailgunHtmlEmailDispatcher implements HtmlEmailDispatcher {

    private final RestClient restClient;
    private final String domain;
    private final String fromEmail;
    private final String fromName;

    public MailgunHtmlEmailDispatcher(
            MailgunProperties mailgunProperties,
            @Value("${spring.mail.from.email}") String fromEmail,
            @Value("${spring.mail.from.name}") String fromName) {
        this.fromEmail = Objects.requireNonNull(fromEmail, "spring.mail.from.email");
        this.fromName = Objects.requireNonNull(fromName, "spring.mail.from.name");
        String baseUrl = Objects.requireNonNull(mailgunProperties.getApiBaseUrl(), "mailgun.api-base-url");
        String apiKey = Objects.requireNonNull(mailgunProperties.getApiKey(), "mailgun.api-key");
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("mailgun.api-key must be set when app.email.transport=mailgun");
        }
        String d = Objects.requireNonNull(mailgunProperties.getDomain(), "mailgun.domain");
        if (!StringUtils.hasText(d)) {
            throw new IllegalStateException("mailgun.domain must be set when app.email.transport=mailgun");
        }
        this.domain = d;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .defaultHeaders(h -> h.setBasicAuth("api", apiKey, StandardCharsets.UTF_8))
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", formatFromHeader());
        form.add("to", to);
        form.add("subject", subject);
        form.add("html", htmlBody);

        restClient.post()
                .uri("/v3/{domain}/messages", domain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    private String formatFromHeader() {
        try {
            return new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid spring.mail.from.email / from.name for Mailgun", e);
        }
    }
}
