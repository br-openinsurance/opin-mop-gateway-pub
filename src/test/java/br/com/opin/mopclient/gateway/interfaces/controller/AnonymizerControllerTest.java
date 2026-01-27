package br.com.opin.mopclient.gateway.interfaces.controller;

import br.com.opin.mopclient.gateway.application.service.JsonPayloadParser;
import br.com.opin.mopclient.gateway.application.service.RabbitMQMessageService;
import br.com.opin.mopclient.gateway.application.service.RequestHeadersBuilder;
import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.interfaces.validation.HeaderValidator;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnonymizerController Tests")
class AnonymizerControllerTest {

    @Mock
    private RabbitMQMessageService messageService;

    @Mock
    private JsonPayloadParser jsonParser;

    @Mock
    private HeaderValidator headerValidator;

    @Mock
    private RequestHeadersBuilder headersBuilder;

    @Mock
    private ResponseBuilder responseBuilder;

    @InjectMocks
    private AnonymizerController controller;

    private String validJsonPayload;
    private JsonNode jsonNode;
    private Map<String, String> headers;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        objectMapper = new ObjectMapper();
        validJsonPayload = "{\"key\":\"value\",\"number\":123}";
        jsonNode = objectMapper.readTree(validJsonPayload);
        headers = new HashMap<>();
        headers.put("custom-header", "custom-value");
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    private RequestHeadersDTO createHeadersDTO(String correlationId, String timestamp) {
        return RequestHeadersDTO.builder()
                .origin("origin-value")
                .destination("destination-value")
                .path("/path")
                .operation("POST")
                .userID("user123")
                .applicationMode("TRANSMITTER")
                .correlationID(correlationId)
                .timestamp(timestamp)
                .headers(headers)
                .build();
    }

    private ApiResponseDTO createSuccessResponse(String correlationId, String timestamp) {
        return ApiResponseDTO.builder()
                .status("SUCCESS")
                .message("Request processed successfully. Your data has been received and forwarded to the queue.")
                .correlationId(correlationId)
                .timestamp(timestamp)
                .origin("origin-value")
                .destination("destination-value")
                .path("/path")
                .operation("POST")
                .applicationMode("TRANSMITTER")
                .build();
    }

    private void setupSuccessfulMocks(String correlationId, String timestamp) throws JsonProcessingException {
        RequestHeadersDTO headersDTO = createHeadersDTO(correlationId, timestamp);
        ApiResponseDTO successResponse = createSuccessResponse(correlationId, timestamp);

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.success());
        when(jsonParser.parse(anyString())).thenReturn(jsonNode);
        when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(validJsonPayload);
        when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(headersDTO);
        doNothing().when(messageService).sendMessageWithHead(anyString(), any());
        when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ResponseEntity.ok(successResponse));
    }

    @Test
    @DisplayName("Deve processar requisição com sucesso quando todos os headers são válidos")
    void shouldProcessRequestSuccessfullyWhenAllHeadersAreValid() throws Exception {
        // Arrange
        setupSuccessfulMocks("test-correlation-id", "2024-01-15T14:30:25.123Z");

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals("Request processed successfully. Your data has been received and forwarded to the queue.", body.getMessage());
        assertNotNull(body.getCorrelationId());
        assertNotNull(body.getTimestamp());
        assertEquals("origin-value", body.getOrigin());
        assertEquals("destination-value", body.getDestination());
        assertEquals("/path", body.getPath());
        assertEquals("POST", body.getOperation());
        assertEquals("TRANSMITTER", body.getApplicationMode());
        assertNull(body.getError());

        verify(messageService, times(1)).sendMessageWithHead(anyString(), any());
        verify(jsonParser, times(1)).parse(validJsonPayload);
        verify(jsonParser, times(1)).toJsonString(any(JsonNode.class));
        verify(headersBuilder, times(1)).build(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any());
        verify(responseBuilder, times(1)).buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando header origin está vazio")
    void shouldReturnBadRequestWhenOriginHeaderIsEmpty() {
        // Arrange
        ApiResponseDTO errorResponse = ApiResponseDTO.builder()
                .status("ERROR")
                .error("Invalid header")
                .details("Header 'origin' must not be empty")
                .timestamp("2024-01-15T14:30:25.123Z")
                .build();

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.error("Header 'origin' must not be empty"));
        when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", "Header 'origin' must not be empty"))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.getStatus());
        assertEquals("Invalid header", body.getError());
        assertEquals("Header 'origin' must not be empty", body.getDetails());
        assertNotNull(body.getTimestamp());

        verify(messageService, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve processar requisição com sucesso quando requestBody está vazio")
    void shouldProcessRequestSuccessfullyWhenRequestBodyIsEmpty() throws Exception {
        // Arrange
        ObjectNode emptyNode = objectMapper.createObjectNode();
        RequestHeadersDTO headersDTO = createHeadersDTO("test-correlation-id", "2024-01-15T14:30:25.123Z");
        ApiResponseDTO successResponse = createSuccessResponse("test-correlation-id", "2024-01-15T14:30:25.123Z");

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.success());
        when(jsonParser.parse("")).thenReturn(emptyNode);
        when(jsonParser.toJsonString(emptyNode)).thenReturn("{}");
        when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(headersDTO);
        doNothing().when(messageService).sendMessageWithHead(anyString(), any());
        when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ResponseEntity.ok(successResponse));

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                "",
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertNotNull(body.getCorrelationId());
        assertNotNull(body.getTimestamp());

        verify(messageService, times(1)).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve retornar erro 500 quando RabbitMQException é lançada")
    void shouldReturnInternalServerErrorWhenRabbitMQExceptionIsThrown() throws Exception {
        // Arrange
        ApiResponseDTO errorResponse = ApiResponseDTO.builder()
                .status("ERROR")
                .error("Message processing error")
                .details("Failed to process message: Error sending message")
                .timestamp("2024-01-15T14:30:25.123Z")
                .build();

        RequestHeadersDTO headersDTO = createHeadersDTO("test-correlation-id", "2024-01-15T14:30:25.123Z");

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.success());
        when(jsonParser.parse(validJsonPayload)).thenReturn(jsonNode);
        when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(validJsonPayload);
        when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(headersDTO);
        doThrow(new RabbitMQException("test-message", "Error sending message", new RuntimeException()))
                .when(messageService).sendMessageWithHead(anyString(), any());
        when(responseBuilder.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Message processing error", anyString()))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.getStatus());
        assertEquals("Message processing error", body.getError());
        assertTrue(body.getDetails().contains("Failed to process message"));
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("Deve gerar correlationId quando não presente no header")
    void shouldGenerateCorrelationIdWhenNotPresentInHeader() throws Exception {
        // Arrange
        ArgumentCaptor<RequestHeadersDTO> headersCaptor = ArgumentCaptor.forClass(RequestHeadersDTO.class);
        RequestHeadersDTO headersDTO = createHeadersDTO("mop-gateway-20240115-143025-123-abc12345", "2024-01-15T14:30:25.123Z");
        ApiResponseDTO successResponse = createSuccessResponse("mop-gateway-20240115-143025-123-abc12345", "2024-01-15T14:30:25.123Z");

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.success());
        when(jsonParser.parse(validJsonPayload)).thenReturn(jsonNode);
        when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(validJsonPayload);
        when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(headersDTO);
        doNothing().when(messageService).sendMessageWithHead(anyString(), headersCaptor.capture());
        when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ResponseEntity.ok(successResponse));

        // Act
        controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        RequestHeadersDTO capturedHeaders = headersCaptor.getValue();
        assertNotNull(capturedHeaders.getCorrelationID());
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando applicationMode é inválido")
    void shouldReturnBadRequestWhenApplicationModeIsInvalid() {
        // Arrange
        ApiResponseDTO errorResponse = ApiResponseDTO.builder()
                .status("ERROR")
                .error("Invalid header")
                .details("Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'")
                .timestamp("2024-01-15T14:30:25.123Z")
                .build();

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.error("Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'"));
        when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", "Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'"))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "INVALID_MODE",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.getStatus());
        assertEquals("Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'", body.getDetails());
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando operation é inválido")
    void shouldReturnBadRequestWhenOperationIsInvalid() {
        // Arrange
        String errorMessage = "Header 'operation' must be one of the following values: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE. Received: 'INVALID_METHOD'";
        ApiResponseDTO errorResponse = ApiResponseDTO.builder()
                .status("ERROR")
                .error("Invalid header")
                .details(errorMessage)
                .timestamp("2024-01-15T14:30:25.123Z")
                .build();

        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.error(errorMessage));
        when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", errorMessage))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "INVALID_METHOD",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.getStatus());
        assertEquals("Invalid header", body.getError());
        assertTrue(body.getDetails().contains("Header 'operation' must be one of the following values"));
        assertTrue(body.getDetails().contains("INVALID_METHOD"));
        assertNotNull(body.getTimestamp());

        verify(messageService, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve processar requisição com sucesso quando operation é GET")
    void shouldProcessRequestSuccessfullyWhenOperationIsGet() throws Exception {
        // Arrange
        setupSuccessfulMocks("test-correlation-id", "2024-01-15T14:30:25.123Z");

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "GET",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        verify(messageService, times(1)).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve processar requisição com sucesso quando operation é PUT")
    void shouldProcessRequestSuccessfullyWhenOperationIsPut() throws Exception {
        // Arrange
        setupSuccessfulMocks("test-correlation-id", "2024-01-15T14:30:25.123Z");

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "PUT",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        verify(messageService, times(1)).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve processar requisição com sucesso quando operation é DELETE")
    void shouldProcessRequestSuccessfullyWhenOperationIsDelete() throws Exception {
        // Arrange
        setupSuccessfulMocks("test-correlation-id", "2024-01-15T14:30:25.123Z");

        // Act
        ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                validJsonPayload,
                "origin-value",
                "destination-value",
                "/path",
                "DELETE",
                "user123",
                "TRANSMITTER",
                headers
        );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        verify(messageService, times(1)).sendMessageWithHead(anyString(), any());
    }
}
