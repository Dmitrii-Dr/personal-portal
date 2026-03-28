package com.dmdr.personal.portal.core.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mailgun HTTP API settings. EU regions use {@code https://api.eu.mailgun.net} as {@code api-base-url}.
 */
@ConfigurationProperties(prefix = "mailgun")
@Component
@Getter
@Setter
public class MailgunProperties {

    /**
     * API root (US default).
     */
    private String apiBaseUrl = "https://api.mailgun.net";

    /**
     * Sending domain (e.g. sandbox domain from Mailgun).
     */
    private String domain = "";

    /**
     * Private API key (secret).
     */
    private String apiKey = "";
}
