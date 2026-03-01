package com.dmdr.personal.portal.service.exception;

public class PersonalPortalRuntimeException extends RuntimeException {

    private final PortalErrorCode errorCode;

    public PersonalPortalRuntimeException(PortalErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PortalErrorCode getErrorCode() {
        return errorCode;
    }
}
