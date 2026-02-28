package com.dmdr.personal.portal.service.exception;

public enum PortalErrorCode {
    EMAIL_ALREADY_IN_USE("PEC-401", 409, "Email already in use"),
    ACCOUNT_NOT_VERIFIED("PEC-402", 403, "Account is not verified. A new verification code was sent to your email."),
    INVALID_OR_EXPIRED_VERIFICATION_CODE("PEC-403", 400, "Invalid or expired verification code.");

    private final String code;
    private final int httpCode;
    private final String message;

    PortalErrorCode(String code, int httpCode, String message) {
        this.code = code;
        this.httpCode = httpCode;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getMessage() {
        return message;
    }
}
