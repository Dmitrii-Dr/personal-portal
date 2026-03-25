package com.dmdr.personal.portal.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Handles missing static resources (e.g. /favicon.ico) without logging as ERROR.
 */
@RestControllerAdvice
@Slf4j
public class StaticResourceExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("No static resource: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }
}
