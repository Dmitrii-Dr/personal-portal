package com.dmdr.personal.portal.admin.observability.classification;

/**
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Phase B1).
 */
public enum HttpOutcomeBucket {
    SUCCESS_2XX,
    AUTH_ERROR_401_403,
    CLIENT_ERROR_4XX_OTHER,
    SERVER_ERROR_5XX,
    OTHER_NON_SUCCESS
}
