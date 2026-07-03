package br.com.opin.mopclient.gateway.interfaces.controller;

import br.com.opin.mopclient.gateway.application.service.JsonPayloadParser;
import br.com.opin.mopclient.gateway.application.service.ProcessingOrchestratorService;
import br.com.opin.mopclient.gateway.application.service.ProcessingResult;
import br.com.opin.mopclient.gateway.application.service.RequestHeadersBuilder;
import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestSummaryDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.ResponseContextDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.ValidationsSummaryDTO;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private static final String HTTP_TYPE = "Request";
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
        when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                .thenReturn(HeaderValidator.ValidationResult.success());
    }

    private void mockBuildAndOrchestrator(RequestHeadersDTO headersDTO) {
        when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString()))
                .thenReturn(headersDTO);
        when(orchestratorService.processRequest(anyString(), anyString(), any()))
                .thenReturn(new ProcessingResult("{}", List.of(), null));
    }

    private RequestHeadersDTO createHeadersDTO(String correlationId, String timestamp) {
        return RequestHeadersDTO.builder()
                .correlationId(correlationId)
                .origin(ORIGIN)
                .path(PATH)
                .operation(OPERATION)
                .traceOrigin(TRACE_ORIGIN_VALUE)
                .mopReportId(correlationId)
                .timestamp(timestamp)
                .clientSSId(CLIENT_SS_ID)
                .serverASId(SERVER_AS_ID)
                .headers(headers)
                .build();
    }

    private ApiResponseDTO createSuccessResponse(String correlationId, String timestamp) {
        return ApiResponseDTO.builder()
                .message(ResponseBuilder.API_SUCCESS_BODY_MESSAGE)
                .timestamp(timestamp)
                .context(ResponseContextDTO.builder()
                        .correlationId(correlationId)
                        .clientSSId(CLIENT_SS_ID)
                        .serverASId(SERVER_AS_ID)
                        .build())
                .request(RequestSummaryDTO.builder().path(PATH).operation(OPERATION).build())
                .validations(ValidationsSummaryDTO.from(List.of()))
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
            when(jsonParser.normalizeRequestBody(VALID_JSON)).thenReturn(VALID_JSON);
            when(jsonParser.parse(VALID_JSON)).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            mockBuildAndOrchestrator(headersDTO);
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyList(), isNull()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(CORRELATION_ID, TIMESTAMP)));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertNotNull(body.getContext());
            assertEquals(CORRELATION_ID, body.getContext().getCorrelationId());
            assertEquals(CLIENT_SS_ID, body.getContext().getClientSSId());
            assertEquals(SERVER_AS_ID, body.getContext().getServerASId());
            assertEquals(PATH, body.getRequest().getPath());
            assertEquals(OPERATION, body.getRequest().getOperation());
            assertNull(body.getError());
            verify(orchestratorService).processRequest(anyString(), anyString(), any());
            verify(jsonParser).parse(VALID_JSON);
            verify(headersBuilder).build(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Processes successfully when request body is empty")
        void shouldProcessRequestSuccessfullyWhenRequestBodyIsEmpty() throws Exception {
            ObjectNode emptyNode = OBJECT_MAPPER.createObjectNode();
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.normalizeRequestBody("")).thenReturn("{}");
            when(jsonParser.parse("{}")).thenReturn(emptyNode);
            when(jsonParser.toJsonString(emptyNode)).thenReturn("{}");
            mockBuildAndOrchestrator(headersDTO);
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyList(), isNull()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(CORRELATION_ID, TIMESTAMP)));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    "", CORRELATION_ID, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            verify(orchestratorService).processRequest(eq("{}"), eq("{}"), any());
        }

        @Test
        @DisplayName("Processes successfully when request body is null")
        void shouldProcessRequestSuccessfullyWhenRequestBodyIsNull() throws Exception {
            ObjectNode emptyNode = OBJECT_MAPPER.createObjectNode();
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.normalizeRequestBody(null)).thenReturn("{}");
            when(jsonParser.parse("{}")).thenReturn(emptyNode);
            when(jsonParser.toJsonString(emptyNode)).thenReturn("{}");
            mockBuildAndOrchestrator(headersDTO);
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyList(), isNull()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(CORRELATION_ID, TIMESTAMP)));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    null, CORRELATION_ID, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(orchestratorService).processRequest(eq("{}"), eq("{}"), any());
        }

        @Test
        @DisplayName("Forwards correlationId from header to DTO and response")
        void shouldPassCorrelationIdFromHeaderToDTOAndResponse() throws Exception {
            ArgumentCaptor<RequestHeadersDTO> captor = ArgumentCaptor.forClass(RequestHeadersDTO.class);
            String correlationIdValue = "my-correlation-123";
            RequestHeadersDTO headersDTO = createHeadersDTO(correlationIdValue, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.normalizeRequestBody(VALID_JSON)).thenReturn(VALID_JSON);
            when(jsonParser.parse(VALID_JSON)).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString()))
                    .thenReturn(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), captor.capture()))
                    .thenReturn(new ProcessingResult("{}", List.of(), null));
            when(responseBuilder.buildSuccessResponse(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyList(), isNull()))
                    .thenReturn(ResponseEntity.ok(createSuccessResponse(correlationIdValue, TIMESTAMP)));

            controller.receivedRequest(
                    VALID_JSON, correlationIdValue, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertNotNull(captor.getValue().getCorrelationId());
            assertEquals(correlationIdValue, captor.getValue().getCorrelationId());
        }

        @Test
        @DisplayName("Returns 202 Accepted with retry-path message when orchestrator enqueues to mop.client.retry.queue")
        void shouldReturnAcceptedWithRetryMessageWhenClientRetryEnqueuedException() throws Exception {
            RequestHeadersDTO headersDTO = createHeadersDTO(CORRELATION_ID, TIMESTAMP);
            mockValidationSuccess();
            when(jsonParser.normalizeRequestBody(VALID_JSON)).thenReturn(VALID_JSON);
            when(jsonParser.parse(VALID_JSON)).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            when(headersBuilder.build(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString()))
                    .thenReturn(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), any()))
                    .thenThrow(new ClientRetryEnqueuedException(headersDTO, ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE));

            ApiResponseDTO retryBody = ApiResponseDTO.builder()
                    .message(ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE)
                    .timestamp(TIMESTAMP)
                    .context(ResponseContextDTO.builder()
                            .correlationId(CORRELATION_ID)
                            .clientSSId(CLIENT_SS_ID)
                            .serverASId(SERVER_AS_ID)
                            .build())
                    .request(RequestSummaryDTO.builder().path(PATH).operation(OPERATION).build())
                    .validations(ValidationsSummaryDTO.from(List.of()))
                    .build();
            when(responseBuilder.buildAcceptedResponse(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                    .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(retryBody));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE, response.getBody().getMessage());
            verify(responseBuilder).buildAcceptedResponse(
                    eq(CORRELATION_ID),
                    eq(TIMESTAMP),
                    eq(CLIENT_SS_ID),
                    eq(SERVER_AS_ID),
                    eq(PATH),
                    eq(OPERATION),
                    any(),
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
            String batchJson = "[{\"a\":1}]";
            mockValidationSuccess();
            when(jsonParser.normalizeRequestBody(batchJson)).thenReturn(batchJson);
            when(jsonParser.parse(batchJson)).thenReturn(arrayRoot);
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid JSON body", details))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder()
                                    .error("Invalid JSON body")
                                    .details(details)
                                    .timestamp(TIMESTAMP)
                                    .build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    "[{\"a\":1}]", CORRELATION_ID, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("Invalid JSON body", body.getError());
            assertEquals(details, body.getDetails());
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
            verify(jsonParser, never()).toJsonString(any());
        }

        @Test
        @DisplayName("Returns 400 when origin header is empty")
        void shouldReturnBadRequestWhenOriginHeaderIsEmpty() {
            when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                    .thenReturn(HeaderValidator.ValidationResult.error("Header 'origin' must not be empty"));
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", "Header 'origin' must not be empty"))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder().error("Invalid header").details("Header 'origin' must not be empty").timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, "", PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("Header 'origin' must not be empty", body.getDetails());
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Returns 400 when origin is invalid (only client or server allowed)")
        void shouldReturnBadRequestWhenOriginIsInvalid() {
            String details = "Header 'origin' must be either 'client' or 'server'";
            when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                    .thenReturn(HeaderValidator.ValidationResult.error(details));
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", details))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder().error("Invalid header").details(details).timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, "INVALID_ORIGIN", PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertTrue(body.getDetails().contains("client"));
            assertTrue(body.getDetails().contains("server"));
            verify(orchestratorService, never()).processRequest(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Returns 400 when operation is invalid")
        void shouldReturnBadRequestWhenOperationIsInvalid() {
            String errorMessage = "Header 'operation' must be one of the following values: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE. Received: 'INVALID_METHOD'";
            when(headerValidator.validate(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString(), anyString()))
                    .thenReturn(HeaderValidator.ValidationResult.error(errorMessage));
            when(responseBuilder.buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", errorMessage))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponseDTO.builder().error("Invalid header").details(errorMessage).timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, "INVALID_METHOD", HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
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
            when(jsonParser.normalizeRequestBody(VALID_JSON)).thenReturn(VALID_JSON);
            when(jsonParser.parse(VALID_JSON)).thenReturn(jsonNode);
            when(jsonParser.toJsonString(any(JsonNode.class))).thenReturn(VALID_JSON);
            mockBuildAndOrchestrator(headersDTO);
            when(orchestratorService.processRequest(anyString(), anyString(), any())).thenThrow(new RuntimeException("Error sending message"));
            when(responseBuilder.buildErrorResponse(eq(HttpStatus.INTERNAL_SERVER_ERROR), eq("Unexpected error"), anyString()))
                    .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponseDTO.builder().error("Unexpected error").details("An unexpected error occurred while processing the request").timestamp(TIMESTAMP).build()));

            ResponseEntity<ApiResponseDTO> response = controller.receivedRequest(
                    VALID_JSON, CORRELATION_ID, ORIGIN, PATH, OPERATION, HTTP_TYPE, null,
                    TRACE_ORIGIN_VALUE, CLIENT_SS_ID, SERVER_AS_ID, headers);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            ApiResponseDTO body = response.getBody();
            assertNotNull(body);
            assertEquals("Unexpected error", body.getError());
        }
    }
}
