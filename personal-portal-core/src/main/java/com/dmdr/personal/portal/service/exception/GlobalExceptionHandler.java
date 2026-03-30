package com.dmdr.personal.portal.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.Map;

import static com.dmdr.personal.portal.service.exception.PortalErrorCode.FILE_TOO_LARGE;
import static com.dmdr.personal.portal.service.exception.PortalErrorCode.UNEXPECTED_SERVER_ERROR;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
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
