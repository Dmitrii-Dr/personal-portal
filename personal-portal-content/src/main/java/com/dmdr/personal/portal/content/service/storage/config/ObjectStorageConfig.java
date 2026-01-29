package com.dmdr.personal.portal.content.service.storage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Configuration for S3-compatible object storage.
 * Supports AWS S3 and any S3-compatible provider (Cloudflare R2, DigitalOcean
 * Spaces, MinIO, etc.)
 * by configuring a custom endpoint URL.
 */
@Configuration
@Slf4j
public class ObjectStorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${cloud.aws.region.static:us-east-1}") String region,
            @Value("${cloud.aws.credentials.access-key:}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key:}") String secretKey,
            @Value("${cloud.aws.s3.endpoint:}") String endpoint,
            @Value("${cloud.aws.s3.path-style-access:false}") boolean pathStyleAccess) {

        var builder = S3Client.builder()
                .region(Region.of(region));

        // Configure custom endpoint for S3-compatible providers (R2, Spaces, MinIO,
        // etc.)
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            log.info("Using custom S3-compatible endpoint: {}", endpoint);
        } else {
            log.info("Using AWS S3 in region: {}", region);
        }

        // Configure path-style access (required by some providers like MinIO)
        if (pathStyleAccess) {
            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();
            builder.serviceConfiguration(s3Config);
            log.info("Path-style access enabled");
        }

        // Only set credentials if they are provided (not empty)
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
            log.info("Using static credentials");
        } else {
            log.info("Using default credentials provider chain");
        }

        return builder.build();
    }
}
