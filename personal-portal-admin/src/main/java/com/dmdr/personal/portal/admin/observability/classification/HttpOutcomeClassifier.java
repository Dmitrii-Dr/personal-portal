package com.dmdr.personal.portal.admin.observability.classification;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B1).
 */
public final class HttpOutcomeClassifier {

    private HttpOutcomeClassifier() {
    }

    public static HttpOutcomeBucket classify(int httpStatus) {
        if (isSuccess(httpStatus)) {
            return HttpOutcomeBucket.SUCCESS_2XX;
        }
        if (httpStatus == 401 || httpStatus == 403) {
            return HttpOutcomeBucket.AUTH_ERROR_401_403;
        }
        if (httpStatus >= 400 && httpStatus <= 499) {
            return HttpOutcomeBucket.CLIENT_ERROR_4XX_OTHER;
        }
        if (httpStatus >= 500 && httpStatus <= 599) {
            return HttpOutcomeBucket.SERVER_ERROR_5XX;
        }
        return HttpOutcomeBucket.OTHER_NON_SUCCESS;
    }

    public static boolean isSuccess(int httpStatus) {
        return httpStatus >= 200 && httpStatus <= 299;
    }
}
