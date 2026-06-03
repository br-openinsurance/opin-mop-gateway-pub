package br.com.opin.mopclient.gateway.interfaces.controller;

import br.com.opin.mopclient.gateway.application.service.JsonPayloadParser;
import br.com.opin.mopclient.gateway.application.service.ProcessingOrchestratorService;
import br.com.opin.mopclient.gateway.application.service.RequestHeadersBuilder;
import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.interfaces.validation.HeaderValidator;
import br.com.opin.mopclient.retry.ClientRetryUserMessages;
import br.com.opin.mopclient.retry.exception.ClientRetryEnqueuedException;
import br.com.opin.mopclient.shared.util.MopReportidContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnonymizerController Tests")
class AnonymizerControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CORRELATION_ID = "test-correlation-id";
    private static final String TIMESTAMP = "2024-01-15T14:30:25.123Z";
    /** Origin header: only {@code client} or {@code server}. */
    private static final String ORIGIN = "client";
    private static final String PATH = "/path";
    private static final String OPERATION = "POST";
    private static final String STEP_VALUE = "consent-created";
    private static final String DATA_EVENTO_STEP_VALUE = "2026-02-23T18:44:29.650942812Z";
    private static final String TRACE_ORIGIN_VALUE = "CLIENT";
    private static final String CLIENT_SS_ID = "RECEIVER A";
    private static final String SERVER_AS_ID = "TRANSMITTER B";
    private static final String VALID_JSON = "{\"key\":\"value\",\"number\":123}";

    @Mock
    private ProcessingOrchestratorService orchestratorService;
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

    private JsonNode jsonNode;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        jsonNode = OBJECT_MAPPER.readTree(VALID_JSON);
        headers = Collections.singletonMap("custom-header", "custom-value");
        MopReportidContext.clear();
    }

    @AfterEach
    void tearDown() {
        MopReportidContext.clear();
    }

    private void mockValidationSuccess() {
        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.success());
    }

    private void mockBuildAndOrchestrator(RequestHeadersDTO headersDTO) {
        when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), anyString(), anyString()))
                .thenReturn(headersDTO);
        when(orchestratorService.processRequest(anyString(), anyString(), any())).thenReturn("{}");
    }

    private RequestHeadersDTO createHeadersDTO(String correlationId, String timestamp) {
        return RequestHeadersDTO.builder()
                .correlationId(correlationId)
                .origin(ORIGIN)
                .path(PATH)
                .operation(OPERATION)
                .step(STEP_VALUE)
                .dataEventoStep(DATA_EVENTO_STEP_VALUE)
                .traceOrigin(TRACE_ORIGIN_VALUE)
                .mopReportid(correlationId)
                .timestamp(timestamp)
                .clientSSId(CLIENT_SS_ID)
                .serverASId(SERVER_AS_ID)
                .headers(headers)
                .build();
    }

    private ApiResponseDTO createSuccessResponse(String correlationId, String timestamp) {
        return ApiResponseDTO.builder()
                .status("SUCCESS")
                .message(ResponseBuilder.API_SUCCESS_BODY_MESSAGE)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .clientSSId(CLIENT_SS_ID)
                .serverASId(SERVER_AS_ID)
                .path(PATH)
                .operation(OPERATION)
                .build();
    }

    @Nested
    @DisplayName("Successful request")
    class SuccessCases {

        @Test
        @DisplayName("Processes successfully when all headers are valid")
        void shouldProcessRequestSuccessfullyWhenAllHeadersAreValid() throws Exception {
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.parse(anyString())).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            mockBuildAndOrchestrator(headersDTO);
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(CORRELATION_ID, TIMESTAMP)));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("SUCCESS", body.getStatus());
            assertEquals(CORRELATION_ID, body.getCorrelationId());
            assertEquals(CLIENT_SS_ID, body.getClientSSId());
            assertEquals(SERVER_AS_ID, body.getServerASId());
            assertEquals(PATH, body.getPath());
            assertEquals(OPERATION, body.getOperation());
            assertNull(body.getError());
            verify(orchestratorService).processRequest(anyString(), anyString(), any());
            verify(jsonParser).parse(VALID_JSON);
            verify(headersBuilder).build(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Processes successfully when request body is empty")
        void shouldProcessRequestSuccessfullyWhenRequestBodyIsEmpty() throws Exception {
            ObjectNode emptyNode = OBJECT_MAPPER.createObjectNode();
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.parse("")).thenReturn(emptyNode);
            when(jsonParser.toJsonString(emptyNode)).thenReturn("{}");
            mockBuildAndOrchestrator(headersDTO);
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(CORRELATION_ID, TIMESTAMP)));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    "", CORRELATION_ID, ORIGIN, PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("SUCCESS", body.getStatus());
            verify(orchestratorService).processRequest(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Processes successfully when step and dataEventoStep are absent (null)")
        void shouldProcessRequestSuccessfullyWhenStepHeadersAreNull() throws Exception {
            RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
                    .correlationId(CORRELATION_ID)
                    .origin(ORIGIN)
                    .path(PATH)
                    .operation(OPERATION)
                    .step(null)
                    .dataEventoStep(null)
                    .traceOrigin(TRACE_ORIGIN_VALUE)
                    .mopReportid(CORRELATION_ID)
                    .timestamp(TIMESTAMP)
                    .clientSSId(CLIENT_SS_ID)
                    .serverASId(SERVER_AS_ID)
                    .headers(headers)
                    .build();
            ArgumentCaptor<String> stepCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> dataEventoCaptor = ArgumentCaptor.forClass(String.class);
            mockValidationSuccess();
            when(jsonParser.parse(anyString())).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            when(headersBuilder.build(
                    anyString(), anyString(), anyString(), anyString(),
                    stepCaptor.capture(), dataEventoCaptor.capture(), any(), any(), anyString(), anyString()))
                    .thenReturn(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), any())).thenReturn("{}");
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(CORRELATION_ID, TIMESTAMP)));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION,
                    null, null, null, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNull(stepCaptor.getValue());
            assertNull(dataEventoCaptor.getValue());
            verify(orchestratorService).processRequest(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Forwards correlationId from header to DTO and response")
        void shouldPassCorrelationIdFromHeaderToDTOAndResponse() throws Exception {
            ArgumentCaptor<RequestHeadersDTO> captor = ArgumentCaptor.forClass(RequestHeadersDTO.class);
            String correlationIdValue = "my-correlation-123";
            RequestHeadersDTO headersDTO = createHeadersDTO(correlationIdValue, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.parse(anyString())).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), anyString(), anyString()))
                    .thenReturn(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), captor.capture())).thenReturn("{}");
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(correlationIdValue, TIMESTAMP)));

            controller.receivedRequest(
                    VALID_JSON, correlationIdValue, ORIGIN, PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertNotNull(captor.getValue().getCorrelationId());
            assertEquals(correlationIdValue, captor.getValue().getCorrelationId());
        }

        @Test
        @DisplayName("Returns 202 Accepted with retry-path message when orchestrator enqueues to mop.client.retry.queue")
        void shouldReturnAcceptedWithRetryMessageWhenClientRetryEnqueuedException() throws Exception {
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.parse(anyString())).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), anyString(), anyString()))
                    .thenReturn(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), any()))
                    .thenThrow(new ClientRetryEnqueuedException(headersDTO, ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE));

            ApiResponseDTO retryBody = ApiResponseDTO.builder()
                    .status("ACCEPTED")
                    .message(ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE)
                    .correlationId(CORRELATION_ID)
                    .timestamp(TIMESTAMP)
                    .clientSSId(CLIENT_SS_ID)
                    .serverASId(SERVER_AS_ID)
                    .path(PATH)
                    .operation(OPERATION)
                    .build();
            when(responseBuilder.buildAcceptedResponse(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(retryBody));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("ACCEPTED", response.getBody().getStatus());
            assertEquals(ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE, response.getBody().getMessage());
            verify(responseBuilder).buildAcceptedResponse(
                    eq(CORRELATION_ID),
                    eq(TIMESTAMP),
                    eq(CLIENT_SS_ID),
                    eq(SERVER_AS_ID),
                    eq(PATH),
                    eq(OPERATION),
                    eq(ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE));
        }
    }

    @Nested
    @DisplayName("Validation errors (400)")
    class ValidationErrors {

        @Test
        @DisplayName("Returns 400 when JSON body root is an array (batch not allowed)")
        void shouldReturnBadRequestWhenBodyRootIsJsonArray() throws Exception {
            String details =
                    "Request body must be a single JSON object. JSON arrays and other root types are not allowed—send one event per HTTP request.";
            JsonNode arrayRoot = OBJECT_MAPPER.readTree("[{\"a\":1},{\"a\":2}]");
            mockValidationSuccess();
            when(jsonParser.parse(anyString())).thenReturn(arrayRoot);
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid JSON body", details))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder()
                                    .status("ERROR")
                                    .error("Invalid JSON body")
                                    .details(details)
                                    .timestamp(TIMESTAMP)
                                    .build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    "[{\"a\":1}]", CORRELATION_ID, ORIGIN, PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("ERROR", body.getStatus());
            assertEquals("Invalid JSON body", body.getError());
            assertEquals(details, body.getDetails());
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
            verify(jsonParser, never()).toJsonString(any());
        }

        @Test
        @DisplayName("Returns 400 when origin header is empty")
        void shouldReturnBadRequestWhenOriginHeaderIsEmpty() {
            when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                    .thenReturn(HeaderValidator.ValidationResult.error("Header 'origin' must not be empty"));
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", "Header 'origin' must not be empty"))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder().status("ERROR").error("Invalid header").details("Header 'origin' must not be empty").timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, "", PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("ERROR", body.getStatus());
            assertEquals("Header 'origin' must not be empty", body.getDetails());
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Returns 400 when origin is invalid (only client or server allowed)")
        void shouldReturnBadRequestWhenOriginIsInvalid() {
            String details = "Header 'origin' must be either 'client' or 'server'";
            when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                    .thenReturn(HeaderValidator.ValidationResult.error(details));
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", details))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder().status("ERROR").error("Invalid header").details(details).timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, "INVALID_ORIGIN", PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("ERROR", body.getStatus());
            assertTrue(body.getDetails().contains("client"));
            assertTrue(body.getDetails().contains("server"));
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Returns 400 when operation is invalid")
        void shouldReturnBadRequestWhenOperationIsInvalid() {
            String errorMessage = "Header 'operation' must be one of the following values: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE. Received: 'INVALID_METHOD'";
            when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                    .thenReturn(HeaderValidator.ValidationResult.error(errorMessage));
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", errorMessage))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder().status("ERROR").error("Invalid header").details(errorMessage).timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, "INVALID_METHOD",
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("ERROR", body.getStatus());
            assertTrue(body.getDetails().contains("INVALID_METHOD"));
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Processing error (500)")
    class ProcessingErrors {

        @Test
        @DisplayName("Returns 500 when an exception is thrown during processing")
        void shouldReturnInternalServerErrorWhenExceptionIsThrown() throws Exception {
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.parse(anyString())).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            mockBuildAndOrchestrator(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), any())).thenThrow(new RuntimeException("Error sending message"));
            when(responseBuilder.buildErrorResponse(eq(HttpStatus.INTERNAL_SERVER_ERROR), eq("Unexpected error"), anyString()))
                    .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponseDTO.builder().status("ERROR").error("Unexpected error").details("An unexpected error occurred while processing the request").timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION,
                    STEP_VALUE, DATA_EVENTO_STEP_VALUE, TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("ERROR", body.getStatus());
            assertEquals("Unexpected error", body.getError());
        }
    }
}
