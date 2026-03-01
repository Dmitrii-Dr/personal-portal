package com.dmdr.personal.portal.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static com.dmdr.personal.portal.service.exception.PortalErrorCode.UNEXPECTED_SERVER_ERROR;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonalPortalRuntimeException.class)
    public ResponseEntity<Map<String, Object>> handlePortalRuntimeException(PersonalPortalRuntimeException e) {
        PortalErrorCode errorCode = e.getErrorCode();
        log.error("Portal exception: {} ({})", e.getMessage(), errorCode.getCode(), e);
        Map<String, Object> error = Map.of(
            "code", errorCode.getCode(),
            "message", errorCode.getMessage()
        );
        return ResponseEntity.status(errorCode.getHttpCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknownException(Exception e) {
        log.error("Illegal argument exception: {}", e.getMessage(), e);
        Map<String, Object> error = Map.of(
            "code", UNEXPECTED_SERVER_ERROR.getCode(),
            "message", UNEXPECTED_SERVER_ERROR.getMessage()
        );

        return ResponseEntity.status(UNEXPECTED_SERVER_ERROR.getHttpCode()).body(error);
    }
}
