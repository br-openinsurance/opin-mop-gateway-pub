package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import br.com.opin.mopclient.retry.infrastructure.outbound.ProcessEndpointCircuitClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalApiClient Tests")
class ExternalApiClientTest {

    @Mock
    private ProcessEndpointCircuitClient processEndpointCircuitClient;

    private ExternalApiClient externalApiClient;

    private static final String TEST_URL = "http://test-server.com/api";
    private static final String VALID_JSON_PAYLOAD = "{\"key\":\"value\"}";

    @BeforeEach
    void setUp() {
        externalApiClient = new ExternalApiClient(processEndpointCircuitClient, TEST_URL);
    }

    @Test
    @DisplayName("Throws NullPointerException when URL is null in constructor")
    void shouldThrowIllegalStateExceptionWhenUrlIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new ExternalApiClient(processEndpointCircuitClient, null);
        });
    }

    @Test
    @DisplayName("Throws IllegalStateException when URL is blank in constructor")
    void shouldThrowIllegalStateExceptionWhenUrlIsBlank() {
        assertThrows(IllegalStateException.class, () -> {
            new ExternalApiClient(processEndpointCircuitClient, "   ");
        });
    }

    @Test
    @DisplayName("Throws NullPointerException when ProcessEndpointCircuitClient is null in constructor")
    void shouldThrowNullPointerExceptionWhenRestTemplateIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new ExternalApiClient(null, TEST_URL);
        });
    }

    @Test
    @DisplayName("Sends JSON payload successfully")
    void shouldSendJsonPayloadSuccessfully() {
        doNothing().when(processEndpointCircuitClient).postJson(eq(TEST_URL), eq(VALID_JSON_PAYLOAD));

        assertDoesNotThrow(() -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD));

        verify(processEndpointCircuitClient, times(1)).postJson(eq(TEST_URL), eq(VALID_JSON_PAYLOAD));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when payload is null, empty, or blank")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsInvalid() {
        String[] invalidPayloads = {null, "", "   "};
        for (String payload : invalidPayloads) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> externalApiClient.sendJsonPayload(payload)
            );
            assertEquals("JSON payload cannot be null or blank", exception.getMessage());
        }
        verify(processEndpointCircuitClient, never()).postJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Propagates ErrorResponseException when the circuit client fails")
    void shouldPropagateErrorResponseExceptionWhenCircuitClientFails() {
        ErrorResponseException cause = new ErrorResponseException("Connection error", "timeout");
        doThrow(cause).when(processEndpointCircuitClient).postJson(eq(TEST_URL), eq(VALID_JSON_PAYLOAD));

        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Connection error", errorResponse.getError());
        verify(processEndpointCircuitClient, times(1)).postJson(eq(TEST_URL), eq(VALID_JSON_PAYLOAD));
    }
}
