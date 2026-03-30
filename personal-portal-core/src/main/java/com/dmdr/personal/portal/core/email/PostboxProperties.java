package com.dmdr.personal.portal.core.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Yandex Cloud Postbox (Amazon SES API v2–compatible) settings for the AWS SDK.
 *
 * <p>Production env vars are summarized in {@code docs/deploy-guide.md} (Postbox / email).
 *
 * @see <a href="https://yandex.cloud/en/docs/postbox/">Yandex Cloud Postbox</a>
 */
@ConfigurationProperties(prefix = "postbox")
@Component
@ConditionalOnProperty(prefix = "app.email", name = "transport", havingValue = "postbox")
@Getter
@Setter
public class PostboxProperties {

    /**
     * SES-compatible HTTPS endpoint (no trailing slash).
     */
    private String endpoint = "https://postbox.cloud.yandex.net";

    /**
     * AWS SigV4 signing region (Yandex Cloud uses {@code ru-central1} for Postbox).
     */
    private String region = "ru-central1";

    /**
     * Static access key id (service account).
     */
    private String accessKeyId = "";

    /**
     * Static secret access key.
     */
    private String secretAccessKey = "";
}
