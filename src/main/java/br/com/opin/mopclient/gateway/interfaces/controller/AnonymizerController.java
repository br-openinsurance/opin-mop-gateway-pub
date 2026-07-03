package br.com.opin.mopclient.gateway.interfaces.controller;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.*;

import br.com.opin.mopclient.gateway.application.service.*;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.interfaces.validation.HeaderValidator;
import br.com.opin.mopclient.retry.exception.ClientRetryEnqueuedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/**
 * Controller for receiving anonymization requests and processing them through unified flow.
 * <p>
 * This controller orchestrates the request processing flow:
 * <ul>
 *   <li>Validates required headers</li>
 *   <li>Parses JSON payload (optional - empty body is treated as {})</li>
 *   <li>Builds request headers DTO with traceability information</li>
 *   <li>Processes request through unified flow: validation -> anonymization -> external API</li>
 *   <li>Returns structured response</li>
 * </ul>
 */
@RestController
@RequestMapping("/data")
public class AnonymizerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymizerController.class);
    private static final DateTimeFormatter LOG_DATE_TIME_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Ingress contract: one business payload per POST. Batching as a JSON array (or other non-object roots)
     * is rejected so clients cannot send multiple MOP-style envelopes in one call.
     */
    private static final String JSON_BODY_ROOT_NOT_OBJECT_DETAILS =
            "Request body must be a single JSON object. JSON arrays and other root types are not allowed—send one event per HTTP request.";
    
    private final ProcessingOrchestratorService orchestratorService;
    private final JsonPayloadParser jsonParser;
    private final HeaderValidator headerValidator;
    private final RequestHeadersBuilder headersBuilder;
    private final ResponseBuilder responseBuilder;

    public AnonymizerController(ProcessingOrchestratorService orchestratorService,
                                JsonPayloadParser jsonParser,
                                HeaderValidator headerValidator,
                                RequestHeadersBuilder headersBuilder,
                                ResponseBuilder responseBuilder) {
        this.orchestratorService = Objects.requireNonNull(orchestratorService, "ProcessingOrchestratorService cannot be null");
        this.jsonParser = Objects.requireNonNull(jsonParser, "JsonPayloadParser cannot be null");
        this.headerValidator = Objects.requireNonNull(headerValidator, "HeaderValidator cannot be null");
        this.headersBuilder = Objects.requireNonNull(headersBuilder, "RequestHeadersBuilder cannot be null");
        this.responseBuilder = Objects.requireNonNull(responseBuilder, "ResponseBuilder cannot be null");
    }

    /**
     * Receives POST request with JSON payload and processes it through unified flow.
     * <p>
     * Body is optional: null/empty or invalid JSON are treated as empty object ({}).
     * If the body parses to a JSON array or any non-object root, the request is rejected with HTTP 400
     * (one business event per request — no batch arrays).
     * Required headers: X-Correlation-Id (correlationId), origin (client/server), path, operation, httpType.
     * Optional: statusCode (obrigatório quando httpType=Response), traceOrigin, clientSSId, serverASId. Opcional: X-Mop-Reportid (gerado se ausente).
     * Response does not include trace object; trace is only present in the final JSON (MessageDTO) sent internally.
     *
     * @param requestBody    JSON payload (can be null or empty)
     * @param correlationId  Correlation ID from header X-Correlation-Id (required, independent field informed by user)
     * @param origin         Origin header - only "client" or "server" (required)
     * @param path           Path header (required)
     * @param operation      Operation header (required)
     * @param httpType         HTTP message type - Request or Response (required)
     * @param statusCode       HTTP status code (required when httpType is Response; optional when Request)
     * @param traceOrigin      Trace event origin, e.g. CLIENT (optional)
     * @param clientSSId     Client SS identifier - receiver (optional)
     * @param serverASId     Server AS identifier - transmitter (optional)
     * @param headers        All request headers
     * @return ResponseEntity with delivered (200), accepted-for-retry (202) or error (400/500)
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO> receivedRequest(
            @RequestBody(required = false) String requestBody,
            @RequestHeader(value = CORRELATION_ID, required = true) String correlationId,
            @RequestHeader(value = ORIGIN, required = true) String origin,
            @RequestHeader(value = PATH, required = true) String path,
            @RequestHeader(value = OPERATION, required = true) String operation,
            @RequestHeader(value = HTTP_TYPE, required = true) String httpType,
            @RequestHeader(value = STATUS_CODE, required = false) String statusCode,
            @RequestHeader(value = TRACE_ORIGIN, required = false) String traceOrigin,
            @RequestHeader(value = CLIENT_SS_ID, required = false) String clientSSId,
            @RequestHeader(value = SERVER_AS_ID, required = false) String serverASId,
            @RequestHeader Map<String, String> headers) {

        LOGGER.debug("Received POST request. Payload length: {}",
                requestBody != null ? requestBody.length() : 0);

        // Validate headers
        HeaderValidator.ValidationResult validationResult = headerValidator.validate(
                correlationId, origin, path, operation, httpType, statusCode,
                clientSSId, serverASId);
        if (!validationResult.isValid()) {
            LOGGER.warn("Header validation failed: {}", validationResult.getErrorMessage());
            return responseBuilder.buildErrorResponse(
                    HttpStatus.BAD_REQUEST, "Invalid header", validationResult.getErrorMessage());
        }

        try {
            String effectiveRequestBody = jsonParser.normalizeRequestBody(requestBody);

            // Parse JSON payload
            JsonNode jsonNode = jsonParser.parse(effectiveRequestBody);
            if (!jsonNode.isObject()) {
                LOGGER.warn(
                        "Rejected request body: root JSON must be an object (not an array or other type) | correlationId={}",
                        correlationId);
                return responseBuilder.buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid JSON body",
                        JSON_BODY_ROOT_NOT_OBJECT_DETAILS);
            }
            String jsonPayload = jsonParser.toJsonString(jsonNode);

            RequestHeadersDTO headersDTO = headersBuilder.build(
                    correlationId, origin, path, operation, httpType, statusCode,
                    traceOrigin,
                    headers, clientSSId, serverASId);

            // Process request through unified flow: validation -> anonymization -> external API
            ProcessingResult processingResult = orchestratorService.processRequest(
                    effectiveRequestBody, jsonPayload, headersDTO);

            String responseClientSSId = headersDTO.getClientSSId() != null && !headersDTO.getClientSSId().isBlank()
                    ? headersDTO.getClientSSId() : headersDTO.getOrigin();
            String responseServerASId = headersDTO.getServerASId() != null && !headersDTO.getServerASId().isBlank()
                    ? headersDTO.getServerASId() : "";

            LOGGER.info("Payload successfully processed. clientSSId: {}, serverASId: {}, correlationId: {}",
                    responseClientSSId, responseServerASId, headersDTO.getCorrelationId());

            // Build and return success response (context echoes request headers; no trace object in response)
            return responseBuilder.buildSuccessResponse(
                    headersDTO.getCorrelationId(),
                    headersDTO.getTimestamp(),
                    responseClientSSId,
                    responseServerASId,
                    headersDTO.getPath(),
                    headersDTO.getOperation(),
                    headersDTO.getHeaders(),
                    processingResult.validations(),
                    processingResult.serverResponse());

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to process JSON: {}", e.getMessage(), e);
            return responseBuilder.buildErrorResponse(
                    HttpStatus.BAD_REQUEST, 
                    "JSON conversion error", 
                    "Failed to process JSON payload: " + e.getMessage());
        } catch (ClientRetryEnqueuedException e) {
            RequestHeadersDTO h = e.getHeadersDTO();
            String responseClientSSId = h.getClientSSId() != null && !h.getClientSSId().isBlank()
                    ? h.getClientSSId() : h.getOrigin();
            String responseServerASId = h.getServerASId() != null && !h.getServerASId().isBlank()
                    ? h.getServerASId() : "";
            String mopReportIdForLog = h.getMopReportId() != null && !h.getMopReportId().isBlank()
                    ? h.getMopReportId()
                    : "-";
            String dateTimeBr = LocalDateTime.now().format(LOG_DATE_TIME_BR);
            LOGGER.warn(
                    "[MOP retry] Client received HTTP 202; body sent to retry queue | correlationId={} | mopReportId={} | dateTime={}",
                    h.getCorrelationId(),
                    mopReportIdForLog,
                    dateTimeBr);
            return responseBuilder.buildAcceptedResponse(
                    h.getCorrelationId(),
                    h.getTimestamp(),
                    responseClientSSId,
                    responseServerASId,
                    h.getPath(),
                    h.getOperation(),
                    h.getHeaders(),
                    e.getSuccessMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing request: {}", e.getMessage(), e);
            return responseBuilder.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Unexpected error", 
                    "An unexpected error occurred while processing the request");
        }
    }
}

