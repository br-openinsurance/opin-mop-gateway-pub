package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalApiClient Tests")
class ExternalApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ExternalApiClient externalApiClient;

    private static final String TEST_URL = "http://test-server.com/api";
    private static final String VALID_JSON_PAYLOAD = "{\"key\":\"value\"}";

    @BeforeEach
    void setUp() {
        externalApiClient = new ExternalApiClient(restTemplate, TEST_URL);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando URL é null no construtor")
    void shouldThrowIllegalStateExceptionWhenUrlIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new ExternalApiClient(restTemplate, null);
        });
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando URL está vazia no construtor")
    void shouldThrowIllegalStateExceptionWhenUrlIsBlank() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            new ExternalApiClient(restTemplate, "   ");
        });
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando RestTemplate é null no construtor")
    void shouldThrowNullPointerExceptionWhenRestTemplateIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new ExternalApiClient(null, TEST_URL);
        });
    }

    @Test
    @DisplayName("Deve enviar payload JSON com sucesso")
    void shouldSendJsonPayloadSuccessfully() {
        // Arrange
        ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenReturn(mockResponse);

        // Act
        assertDoesNotThrow(() -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD));

        // Assert
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload é null")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> externalApiClient.sendJsonPayload(null)
        );

        assertEquals("JSON payload cannot be null or blank", exception.getMessage());
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload está vazio")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> externalApiClient.sendJsonPayload("")
        );

        assertEquals("JSON payload cannot be null or blank", exception.getMessage());
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload contém apenas espaços")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> externalApiClient.sendJsonPayload("   ")
        );

        assertEquals("JSON payload cannot be null or blank", exception.getMessage());
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("Deve lançar ErrorResponseException quando ResourceAccessException ocorre")
    void shouldThrowErrorResponseExceptionWhenResourceAccessExceptionOccurs() {
        // Arrange
        ResourceAccessException exception = new ResourceAccessException("Connection timeout");
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenThrow(exception);

        // Act & Assert
        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Connection error", errorResponse.getError());
        assertTrue(errorResponse.getDetails().contains("The server could not be reached"));
        assertNotNull(errorResponse.getCause());
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve lançar ErrorResponseException quando HttpClientErrorException ocorre (4xx)")
    void shouldThrowErrorResponseExceptionWhenHttpClientErrorExceptionOccurs() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                "Error details".getBytes(),
                null
        );
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenThrow(exception);

        // Act & Assert
        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Client error", errorResponse.getError());
        assertTrue(errorResponse.getDetails().contains("400"));
        assertNotNull(errorResponse.getCause());
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve lançar ErrorResponseException quando HttpServerErrorException ocorre (5xx)")
    void shouldThrowErrorResponseExceptionWhenHttpServerErrorExceptionOccurs() {
        // Arrange
        HttpServerErrorException exception = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                null,
                "Server error details".getBytes(),
                null
        );
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenThrow(exception);

        // Act & Assert
        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Server error", errorResponse.getError());
        assertTrue(errorResponse.getDetails().contains("500"));
        assertNotNull(errorResponse.getCause());
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve lançar ErrorResponseException quando RestClientException genérica ocorre")
    void shouldThrowErrorResponseExceptionWhenGenericRestClientExceptionOccurs() {
        // Arrange
        RestClientException exception = new RestClientException("Generic REST client error");
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenThrow(exception);

        // Act & Assert
        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Request error", errorResponse.getError());
        assertTrue(errorResponse.getDetails().contains("An unexpected error occurred"));
        assertNotNull(errorResponse.getCause());
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve tratar HttpClientErrorException com response body null")
    void shouldHandleHttpClientErrorExceptionWithNullResponseBody() {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "Not Found"
        );
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenThrow(exception);

        // Act & Assert
        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Client error", errorResponse.getError());
        assertTrue(errorResponse.getDetails().contains("N/A"));
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve tratar HttpServerErrorException com response body null")
    void shouldHandleHttpServerErrorExceptionWithNullResponseBody() {
        // Arrange
        HttpServerErrorException exception = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error"
        );
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenThrow(exception);

        // Act & Assert
        ErrorResponseException errorResponse = assertThrows(
                ErrorResponseException.class,
                () -> externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD)
        );

        assertEquals("Server error", errorResponse.getError());
        assertTrue(errorResponse.getDetails().contains("N/A"));
        verify(restTemplate, times(1)).postForEntity(eq(TEST_URL), any(), eq(String.class));
    }

    @Test
    @DisplayName("Deve criar HttpEntity com headers JSON corretos")
    void shouldCreateHttpEntityWithCorrectJsonHeaders() {
        // Arrange
        ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
        when(restTemplate.postForEntity(eq(TEST_URL), any(), eq(String.class)))
                .thenReturn(mockResponse);

        // Act
        externalApiClient.sendJsonPayload(VALID_JSON_PAYLOAD);

        // Assert
        verify(restTemplate, times(1)).postForEntity(
                eq(TEST_URL),
                argThat(entity -> {
                    if (entity instanceof org.springframework.http.HttpEntity) {
                        org.springframework.http.HttpEntity<?> httpEntity = 
                                (org.springframework.http.HttpEntity<?>) entity;
                        return httpEntity.getHeaders().getContentType() != null &&
                               httpEntity.getHeaders().getContentType()
                                       .toString().contains("application/json");
                    }
                    return false;
                }),
                eq(String.class)
        );
    }
}
