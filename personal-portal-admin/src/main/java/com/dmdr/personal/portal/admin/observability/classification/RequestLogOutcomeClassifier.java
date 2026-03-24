package com.dmdr.personal.portal.admin.observability.classification;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B2).
 */
public final class RequestLogOutcomeClassifier {

    private RequestLogOutcomeClassifier() {
    }

    public static RequestLogOutcomeClass fromHttpStatus(int httpStatus) {
        return HttpOutcomeClassifier.isSuccess(httpStatus)
            ? RequestLogOutcomeClass.SUCCESS
            : RequestLogOutcomeClass.FAILURE;
    }
}
