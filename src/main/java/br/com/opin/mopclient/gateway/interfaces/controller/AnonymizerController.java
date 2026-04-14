package br.com.opin.mopclient.gateway.interfaces.controller;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.*;

import br.com.opin.mopclient.gateway.application.service.JsonPayloadParser;
import br.com.opin.mopclient.gateway.application.service.ProcessingOrchestratorService;
import br.com.opin.mopclient.gateway.application.service.RequestHeadersBuilder;
import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;
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
     * Required headers: X-Correlation-Id (correlationId), origin (client/server), path, operation, step, dataEventoStep, clientSSId, serverASId.
     * Optional: destination, traceOrigin (default CLIENT).
     * Response does not include trace object; trace is only present in the final JSON (MessageDTO) sent internally.
     *
     * @param requestBody    JSON payload (can be null or empty)
     * @param correlationId  Correlation ID from header X-Correlation-Id (required, independent field informed by user)
     * @param origin         Origin header - only "client" or "server" (required)
     * @param path           Path header (required)
     * @param operation      Operation header (required)
     * @param step           Step of the flow in the trace, e.g. consent-created (required)
     * @param dataEventoStep Timestamp of the step event, ISO-8601 (required)
     * @param clientSSId     Client SS identifier - receiver (required)
     * @param serverASId     Server AS identifier - transmitter (required)
     * @param headers        All request headers
     * @return ResponseEntity with success (200) or error (400/500)
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO> receivedRequest(
            @RequestBody String requestBody,
            @RequestHeader(value = CORRELATION_ID, required = true) String correlationId,
            @RequestHeader(value = ORIGIN, required = true) String origin,
            @RequestHeader(value = PATH, required = true) String path,
            @RequestHeader(value = OPERATION, required = true) String operation,
            @RequestHeader(value = STEP, required = true) String step,
            @RequestHeader(value = DATA_EVENTO_STEP, required = true) String dataEventoStep,
            @RequestHeader(value = CLIENT_SS_ID, required = true) String clientSSId,
            @RequestHeader(value = SERVER_AS_ID, required = true) String serverASId,
            @RequestHeader Map<String, String> headers) {

        LOGGER.debug("Received POST request. Payload length: {}",
                requestBody != null ? requestBody.length() : 0);

        // Validate headers (correlationId required; origin must be "client" or "server")
        HeaderValidator.ValidationResult validationResult = headerValidator.validate(
                correlationId, origin, path, operation, step, dataEventoStep, clientSSId, serverASId);
        if (!validationResult.isValid()) {
            LOGGER.warn("Header validation failed: {}", validationResult.getErrorMessage());
            return responseBuilder.buildErrorResponse(
                    HttpStatus.BAD_REQUEST, "Invalid header", validationResult.getErrorMessage());
        }

        try {
            // Parse JSON payload
            JsonNode jsonNode = jsonParser.parse(requestBody);
            String jsonPayload = jsonParser.toJsonString(jsonNode);

            // Build headers DTO (correlationId is independent, from user header)
            RequestHeadersDTO headersDTO = headersBuilder.build(
                    correlationId, origin, path, operation,
                    step, dataEventoStep,
                    headers, clientSSId, serverASId);

            // Process request through unified flow: validation -> anonymization -> external API
            orchestratorService.processRequest(requestBody, jsonPayload, headersDTO);

            String responseClientSSId = headersDTO.getClientSSId() != null && !headersDTO.getClientSSId().isBlank()
                    ? headersDTO.getClientSSId() : headersDTO.getOrigin();
            String responseServerASId = headersDTO.getServerASId() != null && !headersDTO.getServerASId().isBlank()
                    ? headersDTO.getServerASId() : (headersDTO.getDestination() != null ? headersDTO.getDestination() : "");

            LOGGER.info("Payload successfully processed. clientSSId: {}, serverASId: {}, correlationId: {}",
                    responseClientSSId, responseServerASId, headersDTO.getCorrelationId());

            // Build and return success response (no trace object in response; only flat fields including correlationId)
            return responseBuilder.buildSuccessResponse(
                    headersDTO.getCorrelationId(),
                    headersDTO.getTimestamp(),
                    responseClientSSId,
                    responseServerASId,
                    path,
                    operation);

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
                    ? h.getServerASId() : (h.getDestination() != null ? h.getDestination() : "");
            String mopReportIdForLog = h.getMopReportid() != null && !h.getMopReportid().isBlank()
                    ? h.getMopReportid()
                    : "-";
            String dateTimeBr = LocalDateTime.now().format(LOG_DATE_TIME_BR);
            LOGGER.warn(
                    "[MOP retry] Client received HTTP 200; body sent to retry queue | correlationId={} | mopReportId={} | dateTime={}",
                    h.getCorrelationId(),
                    mopReportIdForLog,
                    dateTimeBr);
            return responseBuilder.buildSuccessResponse(
                    h.getCorrelationId(),
                    h.getTimestamp(),
                    responseClientSSId,
                    responseServerASId,
                    h.getPath(),
                    h.getOperation(),
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

