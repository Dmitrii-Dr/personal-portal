package com.dmdr.personal.portal.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.Map;

import static com.dmdr.personal.portal.service.exception.PortalErrorCode.FILE_TOO_LARGE;
import static com.dmdr.personal.portal.service.exception.PortalErrorCode.INVALID_SLUG_FORMAT;
import static com.dmdr.personal.portal.service.exception.PortalErrorCode.UNEXPECTED_SERVER_ERROR;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String INVALID_SLUG_VALIDATION_MESSAGE =
        "Slug must contain only lowercase letters, numbers, and hyphens (no spaces or special characters)";

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        // Client disconnected while an async response was being written (common with SSE/tab close).
        // Silently ignore to avoid trying to serialize error JSON into `text/event-stream`.
        return ResponseEntity.noContent().build();
    }

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError firstFieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String validationMessage = firstFieldError != null ? firstFieldError.getDefaultMessage() : null;

        if (INVALID_SLUG_VALIDATION_MESSAGE.equals(validationMessage)) {
            log.warn("Validation failed: {}", validationMessage);
            Map<String, Object> error = Map.of(
                "code", INVALID_SLUG_FORMAT.getCode(),
                "message", INVALID_SLUG_FORMAT.getMessage()
            );
            return ResponseEntity.status(INVALID_SLUG_FORMAT.getHttpCode()).body(error);
        }

        log.warn("Validation failed: {}", validationMessage);
        Map<String, Object> error = Map.of(
            "code", UNEXPECTED_SERVER_ERROR.getCode(),
            "message", validationMessage != null ? validationMessage : UNEXPECTED_SERVER_ERROR.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException e) {
        if (e.getCause() instanceof FileSizeLimitExceededException) {
            log.warn("File upload too large: {}", e.getMessage());
            Map<String, Object> error = Map.of(
                "code", FILE_TOO_LARGE.getCode(),
                "message", FILE_TOO_LARGE.getMessage()
            );
            return ResponseEntity.status(FILE_TOO_LARGE.getHttpCode()).body(error);
        }

        log.error("Illegal state exception: {}", e.getMessage(), e);
        Map<String, Object> error = Map.of(
            "code", UNEXPECTED_SERVER_ERROR.getCode(),
            "message", UNEXPECTED_SERVER_ERROR.getMessage()
        );
        return ResponseEntity.status(UNEXPECTED_SERVER_ERROR.getHttpCode()).body(error);
    }
}
