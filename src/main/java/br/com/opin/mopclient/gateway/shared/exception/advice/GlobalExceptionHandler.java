package br.com.opin.mopclient.gateway.shared.exception.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Global exception handler for centralized error handling in the application.
 * Handles: MissingRequestHeaderException, HttpMessageNotReadableException,
 * and all other exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing request headers.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<List<String>> handleMissingHeader(MissingRequestHeaderException ex) {
        logger.warn("Required header missing: {}", ex.getHeaderName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(List.of("Missing required header: " + ex.getHeaderName()));
    }

    /**
     * Handles unreadable or malformed JSON in the request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        logger.warn("Invalid or unreadable JSON payload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(List.of("Invalid or unreadable JSON payload."));
    }

    /**
     * Unknown URL: Spring would otherwise wrap {@link NoResourceFoundException} as HTTP 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<List<String>> handleNoResourceFound(NoResourceFoundException ex) {
        logger.warn("Endpoint not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(List.of("Endpoint not found."));
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<List<String>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of("An unexpected error occurred."));
    }
}
