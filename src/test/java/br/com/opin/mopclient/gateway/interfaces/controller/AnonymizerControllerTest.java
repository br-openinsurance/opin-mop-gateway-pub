package br.com.opin.mopclient.gateway.interfaces.controller;

import br.com.opin.mopclient.gateway.application.usecase.DataAnonymizerUseCase;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnonymizerController Tests")
class AnonymizerControllerTest {

    @Mock
    private DataAnonymizerUseCase dataAnonymizerUseCase;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnonymizerController controller;

    private String validJsonPayload;
    private JsonNode jsonNode;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        validJsonPayload = "{\"key\":\"value\",\"number\":123}";
        jsonNode = new ObjectMapper().readTree(validJsonPayload);
        headers = new HashMap<>();
        headers.put("custom-header", "custom-value");
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    @DisplayName("Deve processar requisição com sucesso quando todos os headers são válidos")
    void shouldProcessRequestSuccessfullyWhenAllHeadersAreValid() throws Exception {
        // Arrange
        when(objectMapper.readTree(validJsonPayload)).thenReturn(jsonNode);
        when(objectMapper.writeValueAsString(any(JsonNode.class))).thenReturn(validJsonPayload);
        doNothing().when(dataAnonymizerUseCase).sendMessageWithHead(anyString(), any());

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Message sent successfully", body.getMessage());
        assertNull(body.getError());
        
        verify(dataAnonymizerUseCase, times(1)).sendMessageWithHead(anyString(), any());
        verify(objectMapper, times(1)).readTree(validJsonPayload);
        verify(objectMapper, times(1)).writeValueAsString(any(JsonNode.class));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando header origin está vazio")
    void shouldReturnBadRequestWhenOriginHeaderIsEmpty() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid header", body.getError());
        assertEquals("Header 'origin' must not be empty", body.getDetails());
        
        verify(dataAnonymizerUseCase, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando header destination está vazio")
    void shouldReturnBadRequestWhenDestinationHeaderIsEmpty() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Header 'destination' must not be empty", body.getDetails());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando header path está vazio")
    void shouldReturnBadRequestWhenPathHeaderIsEmpty() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Header 'path' must not be empty", body.getDetails());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando header operation está vazio")
    void shouldReturnBadRequestWhenOperationHeaderIsEmpty() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Header 'operation' must not be empty", body.getDetails());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando header userID está vazio")
    void shouldReturnBadRequestWhenUserIDHeaderIsEmpty() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Header 'userID' must not be empty", body.getDetails());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando requestBody está vazio")
    void shouldReturnBadRequestWhenRequestBodyIsEmpty() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                "",
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid JSON", body.getError());
        assertEquals("Invalid or empty JSON payload", body.getDetails());
        
        verify(dataAnonymizerUseCase, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando requestBody é null")
    void shouldReturnBadRequestWhenRequestBodyIsNull() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                null,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid JSON", body.getError());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando JSON é inválido")
    void shouldReturnBadRequestWhenJsonIsInvalid() throws Exception {
        // Arrange
        when(objectMapper.readTree("invalid-json")).thenThrow(new JsonProcessingException("Invalid JSON") {});

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                "invalid-json",
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid JSON", body.getError());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando JSON está vazio")
    void shouldReturnBadRequestWhenJsonIsEmpty() throws Exception {
        // Arrange
        JsonNode emptyNode = new ObjectMapper().readTree("{}");
        when(objectMapper.readTree("{}")).thenReturn(emptyNode);

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                "{}",
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid JSON", body.getError());
    }

    @Test
    @DisplayName("Deve retornar erro 500 quando RabbitMQException é lançada")
    void shouldReturnInternalServerErrorWhenRabbitMQExceptionIsThrown() throws Exception {
        // Arrange
        when(objectMapper.readTree(validJsonPayload)).thenReturn(jsonNode);
        when(objectMapper.writeValueAsString(any(JsonNode.class))).thenReturn(validJsonPayload);
        doThrow(new RabbitMQException("test-message", "Error sending message", new RuntimeException()))
                .when(dataAnonymizerUseCase).sendMessageWithHead(anyString(), any());

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Message processing error", body.getError());
        assertTrue(body.getDetails().contains("Failed to process message"));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando JsonProcessingException é lançada na conversão")
    void shouldReturnBadRequestWhenJsonProcessingExceptionIsThrown() throws Exception {
        // Arrange
        when(objectMapper.readTree(validJsonPayload)).thenReturn(jsonNode);
        when(objectMapper.writeValueAsString(any(JsonNode.class)))
                .thenThrow(new JsonProcessingException("Conversion error") {});

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("JSON conversion error", body.getError());
        assertTrue(body.getDetails().contains("Failed to process JSON payload"));
    }

    @Test
    @DisplayName("Deve retornar erro 500 quando exceção inesperada é lançada")
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionIsThrown() throws Exception {
        // Arrange
        when(objectMapper.readTree(validJsonPayload)).thenReturn(jsonNode);
        when(objectMapper.writeValueAsString(any(JsonNode.class))).thenReturn(validJsonPayload);
        doThrow(new RuntimeException("Unexpected error"))
                .when(dataAnonymizerUseCase).sendMessageWithHead(anyString(), any());

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Unexpected error", body.getError());
    }

    @Test
    @DisplayName("Deve validar headers com espaços em branco")
    void shouldValidateHeadersWithWhitespace() {
        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "   ",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Header 'origin' must not be empty", body.getDetails());
    }

    @Test
    @DisplayName("Deve definir correlationId no MDC quando presente no header")
    void shouldSetCorrelationIdInMDCWhenPresentInHeader() throws Exception {
        // Arrange
        String correlationId = "test-correlation-id-123";
        headers.put(CORRELATIONID, correlationId);
        when(objectMapper.readTree(validJsonPayload)).thenReturn(jsonNode);
        when(objectMapper.writeValueAsString(any(JsonNode.class))).thenReturn(validJsonPayload);
        doNothing().when(dataAnonymizerUseCase).sendMessageWithHead(anyString(), any());

        // Act
        controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        assertEquals(correlationId, CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve gerar correlationId quando não presente no header")
    void shouldGenerateCorrelationIdWhenNotPresentInHeader() throws Exception {
        // Arrange
        when(objectMapper.readTree(validJsonPayload)).thenReturn(jsonNode);
        when(objectMapper.writeValueAsString(any(JsonNode.class))).thenReturn(validJsonPayload);
        doNothing().when(dataAnonymizerUseCase).sendMessageWithHead(anyString(), any());

        // Act
        controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "certificate-value",
                "user123",
                headers
        );

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }
}

