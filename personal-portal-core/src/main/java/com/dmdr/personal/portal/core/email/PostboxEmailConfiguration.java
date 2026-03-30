package com.dmdr.personal.portal.core.email;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * {@link SesV2Client} aimed at Yandex Cloud Postbox ({@link PostboxProperties#getEndpoint()}).
 *
 * <p>Postbox implements the <strong>SES API v2</strong> ({@code SendEmail}); classic {@code SesClient} hits
 * different paths and returns HTTP 404 on the Postbox endpoint.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.email", name = "transport", havingValue = "postbox")
public class PostboxEmailConfiguration {

    @Bean(destroyMethod = "close")
    public SesV2Client postboxSesClient(PostboxProperties postboxProperties) {
        String accessKey = Objects.requireNonNull(postboxProperties.getAccessKeyId(), "postbox.access-key-id");
        String secretKey =
                Objects.requireNonNull(postboxProperties.getSecretAccessKey(), "postbox.secret-access-key");
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "postbox.access-key-id and postbox.secret-access-key must be set when app.email.transport=postbox");
        }
        String endpoint = Objects.requireNonNull(postboxProperties.getEndpoint(), "postbox.endpoint");
        String region = Objects.requireNonNull(postboxProperties.getRegion(), "postbox.region");
        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("postbox.region must not be empty");
        }
        return SesV2Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(trimTrailingSlash(endpoint)))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
