package com.dmdr.personal.portal.service.exception;

public enum PortalErrorCode {
    EMAIL_ALREADY_IN_USE("PEC-411", 409, "Email already in use"),
    ACCOUNT_NOT_VERIFIED("PEC-412", 403, "Account is not verified."),
    INVALID_OR_EXPIRED_VERIFICATION_CODE("PEC-413", 400, "Invalid or expired verification code. Try again or request a new verification code.");

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
