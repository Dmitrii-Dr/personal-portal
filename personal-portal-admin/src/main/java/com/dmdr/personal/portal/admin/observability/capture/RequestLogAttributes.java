package com.dmdr.personal.portal.admin.observability.capture;

/**
 * Request attribute keys used by the request-log capture pipeline.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase C1, C2).
 */
public final class RequestLogAttributes {

    public static final String CAPTURE_CONTEXT = RequestLogAttributes.class.getName() + ".CAPTURE_CONTEXT";
    public static final String ERROR_CODE = RequestLogAttributes.class.getName() + ".ERROR_CODE";
    public static final String ERROR_MESSAGE = RequestLogAttributes.class.getName() + ".ERROR_MESSAGE";
    public static final String STACK_TRACE = RequestLogAttributes.class.getName() + ".STACK_TRACE";

    private RequestLogAttributes() {
    }
}
