package com.dmdr.personal.portal.admin.observability.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.dmdr.personal.portal.admin.observability.classification.HttpOutcomeClassifier;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

class DefaultRequestLoggingPathPolicyTest {

    private final DefaultRequestLoggingPathPolicy policy = new DefaultRequestLoggingPathPolicy();

    @Test
    void shouldCaptureAtAll_shouldSkipActuatorOnly() {
        assertThat(policy.shouldCaptureAtAll("/actuator/health")).isFalse();
        assertThat(policy.shouldCaptureAtAll("/api/v1/bookings")).isTrue();
        assertThat(policy.shouldCaptureAtAll("/api/v1/admin/users")).isTrue();
    }

    @Test
    void shouldSkipSuccess_shouldMatchAdminPrefixOnly() {
        assertThat(policy.shouldSkipSuccess("/admin/sba/applications")).isTrue();
        assertThat(policy.shouldSkipSuccess("/api/v1/admin/users")).isFalse();
    }

    @Test
    void isProbablyStaticAsset_shouldUseSuffixesOrResourceHandler() {
        assertThat(policy.isProbablyStaticAsset("/admin/assets/main.css", null)).isTrue();
        assertThat(policy.isProbablyStaticAsset("/api/v1/bookings", null)).isFalse();
        assertThat(policy.isProbablyStaticAsset("/any/path", new ResourceHttpRequestHandler())).isTrue();
    }

    @Test
    void adminSuccessVsError_shouldCaptureOnlyErrorWhenConfiguredToSkipSuccess() {
        String adminPath = "/admin/sba/instances";

        boolean captureSuccess = shouldCapture(adminPath, 200);
        boolean captureServerError = shouldCapture(adminPath, 500);

        assertThat(captureSuccess).isFalse();
        assertThat(captureServerError).isTrue();
    }

    private boolean shouldCapture(String path, int status) {
        if (!policy.shouldCaptureAtAll(path)) {
            return false;
        }
        return !(
            policy.shouldSkipSuccess(path)
                && HttpOutcomeClassifier.isSuccess(status)
        );
    }
}
