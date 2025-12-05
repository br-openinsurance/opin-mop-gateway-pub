package br.com.opin.mopclient.gateway.shared.exception.advice;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles missing request headers.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<List<String>> handleMissingHeader(MissingRequestHeaderException ex) {
        List<String> errors = List.of(
                "Missing required header: " + ex.getHeaderName(),
                "Details: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles unreadable or malformed JSON in the request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        List<String> errors = List.of(
                "Invalid or unreadable JSON payload.",
                "Details: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<List<String>> handleGenericException(Exception ex) {
        List<String> errors = List.of(
                "An unexpected error occurred.",
                "Details: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }
}
