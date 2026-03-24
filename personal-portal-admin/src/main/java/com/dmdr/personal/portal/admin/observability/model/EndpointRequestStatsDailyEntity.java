package com.dmdr.personal.portal.admin.observability.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "endpoint_stats")
@IdClass(EndpointStatsId.class)
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class EndpointRequestStatsDailyEntity {

    @Id
    @Column(name = "bucket_start", nullable = false)
    private LocalDate bucketStart;

    @Id
    @Column(nullable = false, length = 16)
    private String method;

    @Id
    @Column(name = "template_path", nullable = false, length = 2048)
    private String templatePath;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "success_count", nullable = false)
    private long successCount;

    @Column(name = "auth_error_count", nullable = false)
    private long authErrorCount;

    @Column(name = "client_error_count", nullable = false)
    private long clientErrorCount;

    @Column(name = "server_error_count", nullable = false)
    private long serverErrorCount;

    @Column(name = "other_non_success_count", nullable = false)
    private long otherNonSuccessCount;
}
