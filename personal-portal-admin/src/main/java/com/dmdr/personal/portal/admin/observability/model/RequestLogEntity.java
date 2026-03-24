package com.dmdr.personal.portal.admin.observability.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "request_log")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"requestBody", "requestHeaders", "responseHeaders", "stackTrace"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 2048)
    private String path;

    @Column(name = "template_path", nullable = false, length = 2048)
    private String templatePath;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "request_body")
    private String requestBody;

    @Column(name = "request_headers")
    private String requestHeaders;

    @Column(name = "response_headers")
    private String responseHeaders;

    @Column(name = "stack_trace")
    private String stackTrace;
}
