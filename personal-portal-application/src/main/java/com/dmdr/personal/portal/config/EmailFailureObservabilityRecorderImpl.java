package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.admin.observability.persistence.RequestLogPersistenceGateway;
import com.dmdr.personal.portal.admin.observability.persistence.RequestLogRecord;
import com.dmdr.personal.portal.core.email.EmailFailureObservabilityRecorder;
import com.dmdr.personal.portal.core.email.EmailRequestContextSnapshot;
import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Application bridge that persists dedicated observability records for async email failures.
 * See docs/observability/dev/embedded-sba-actuator-design.md.
 */
@Slf4j
@Primary
@Component
public class EmailFailureObservabilityRecorderImpl implements EmailFailureObservabilityRecorder {

    private static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    private final RequestLogPersistenceGateway requestLogPersistenceGateway;

    public EmailFailureObservabilityRecorderImpl(RequestLogPersistenceGateway requestLogPersistenceGateway) {
        this.requestLogPersistenceGateway =
            Objects.requireNonNull(requestLogPersistenceGateway, "requestLogPersistenceGateway");
    }

    @Override
    public void recordFailure(
        EmailRequestContextSnapshot contextSnapshot,
        String recipient,
        String emailType,
        PersonalPortalRuntimeException portalException,
        Throwable cause
    ) {
        Objects.requireNonNull(contextSnapshot, "contextSnapshot");
        String errorMessage = portalException.getMessage() + " (type=" + emailType + ", recipient=" + recipient + ")";
        RequestLogRecord record = new RequestLogRecord(
            contextSnapshot.path(),
            contextSnapshot.path(),
            contextSnapshot.method(),
            STATUS_INTERNAL_SERVER_ERROR,
            0L,
            contextSnapshot.userId(),
            Instant.now(),
            portalException.getErrorCode().getCode(),
            errorMessage,
            null,
            null,
            null,
            toStackTrace(cause != null ? cause : portalException)
        );
        requestLogPersistenceGateway.enqueue(record);
        log.info(
            "Persisted async email failure record: path={}, method={}, code={}, type={}, recipient={}",
            contextSnapshot.path(),
            contextSnapshot.method(),
            portalException.getErrorCode().getCode(),
            emailType,
            recipient
        );
    }

    private static String toStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
