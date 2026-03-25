package com.dmdr.personal.portal.admin.observability.routing;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B3).
 */
public interface RequestLoggingPathPolicy {

    boolean shouldCaptureAtAll(String path);

    boolean shouldSkipSuccess(String path);

    boolean isProbablyStaticAsset(String path, Object handlerOrNull);
}
