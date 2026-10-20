package br.com.opin.mopclient.gateway.shared.exception.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("missing header response names the header without Spring exception text")
    void missingHeaderDoesNotLeakExceptionMessage() {
        var ex = new MissingRequestHeaderException("origin", mock(MethodParameter.class));

        var response = handler.handleMissingHeader(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of("Missing required header: origin"), response.getBody());
        assertFalse(String.join(" ", response.getBody()).contains(ex.getMessage()));
        assertFalse(String.join(" ", response.getBody()).contains("Required request header"));
    }

    @Test
    @DisplayName("unreadable JSON response stays generic")
    void unreadableBodyDoesNotLeakParserDetails() {
        var ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character", mock(HttpInputMessage.class));

        var response = handler.handleUnreadableBody(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of("Invalid or unreadable JSON payload."), response.getBody());
    }

    @Test
    @DisplayName("unknown URL response does not echo method or path")
    void notFoundDoesNotEchoResource() {
        var ex = new NoResourceFoundException(HttpMethod.GET, "", "/actuator/env");

        var response = handler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(List.of("Endpoint not found."), response.getBody());
    }

    @Test
    @DisplayName("unexpected error response does not include exception message")
    void genericErrorDoesNotLeakInternalMessage() {
        var ex = new RuntimeException("jdbc:postgresql://internal-db:5432/mop");

        var response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(List.of("An unexpected error occurred."), response.getBody());
    }
}
