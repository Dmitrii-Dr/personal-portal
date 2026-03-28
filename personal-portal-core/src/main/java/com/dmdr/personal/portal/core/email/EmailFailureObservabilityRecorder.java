package com.dmdr.personal.portal.core.email;

import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;

/**
 * Emits dedicated observability records for async email failures.
 * See docs/observability/dev/embedded-sba-actuator-design.md.
 */
public interface EmailFailureObservabilityRecorder {

    void recordFailure(
        EmailRequestContextSnapshot contextSnapshot,
        String recipient,
        String emailType,
        PersonalPortalRuntimeException portalException,
        Throwable cause
    );
}
