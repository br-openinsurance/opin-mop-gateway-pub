package br.com.opin.mopclient.gateway.shared.exception;

import lombok.Getter;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class ErrorResponseException extends RuntimeException {

    private final String error;
    private final String details;
    private final String timestamp;

    public ErrorResponseException(String error, String details) {
        super(buildMessage(error, details));
        this.error = error;
        this.details = details;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public ErrorResponseException(String error, String details, Throwable cause) {
        super(buildMessage(error, details), cause);
        this.error = error;
        this.details = details;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String buildMessage(String error, String details) {
        return String.format("%s - %s", error, details);
    }

}
