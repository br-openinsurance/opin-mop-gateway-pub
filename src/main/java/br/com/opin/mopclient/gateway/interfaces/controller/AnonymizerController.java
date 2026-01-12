package br.com.opin.mopclient.gateway.interfaces.controller;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.*;

import br.com.opin.mopclient.gateway.application.service.JsonPayloadParser;
import br.com.opin.mopclient.gateway.application.service.RabbitMQMessageService;
import br.com.opin.mopclient.gateway.application.service.RequestHeadersBuilder;
import br.com.opin.mopclient.gateway.application.service.ResponseBuilder;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.interfaces.validation.HeaderValidator;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

/**
 * Controller for receiving anonymization requests and forwarding them to RabbitMQ.
 * <p>
 * This controller orchestrates the request processing flow:
 * <ul>
 *   <li>Validates required headers</li>
 *   <li>Parses JSON payload (optional - empty body is treated as {})</li>
 *   <li>Builds request headers DTO with traceability information</li>
 *   <li>Forwards message to RabbitMQ</li>
 *   <li>Returns structured response</li>
 * </ul>
 */
@RestController
@RequestMapping("/data")
public class AnonymizerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymizerController.class);
    
    private final RabbitMQMessageService messageService;
    private final JsonPayloadParser jsonParser;
    private final HeaderValidator headerValidator;
    private final RequestHeadersBuilder headersBuilder;
    private final ResponseBuilder responseBuilder;

    public AnonymizerController(RabbitMQMessageService messageService,
                                JsonPayloadParser jsonParser,
                                HeaderValidator headerValidator,
                                RequestHeadersBuilder headersBuilder,
                                ResponseBuilder responseBuilder) {
        this.messageService = Objects.requireNonNull(messageService, "RabbitMQMessageService cannot be null");
        this.jsonParser = Objects.requireNonNull(jsonParser, "JsonPayloadParser cannot be null");
        this.headerValidator = Objects.requireNonNull(headerValidator, "HeaderValidator cannot be null");
        this.headersBuilder = Objects.requireNonNull(headersBuilder, "RequestHeadersBuilder cannot be null");
        this.responseBuilder = Objects.requireNonNull(responseBuilder, "ResponseBuilder cannot be null");
    }

    /**
     * Receives POST request with JSON payload and forwards it to RabbitMQ.
     * <p>
     * Body is optional: null/empty or invalid JSON are treated as empty object ({}).
     *
     * @param requestBody JSON payload (can be null or empty)
     * @param origin Origin header (required)
     * @param destination Destination header (required)
     * @param path Path header (required)
     * @param operation Operation header (required)
     * @param userID UserID header (required)
     * @param applicationMode ApplicationMode header - TRANSMITTER or RECEIVER (required)
     * @param headers All request headers
     * @return ResponseEntity with success (200) or error (400/500)
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO> receivedRequest(
            @RequestBody String requestBody,
            @RequestHeader(value = ORIGIN, required = true) String origin,
            @RequestHeader(value = DESTINATION, required = true) String destination,
            @RequestHeader(value = PATH, required = true) String path,
            @RequestHeader(value = OPERATION, required = true) String operation,
            @RequestHeader(value = USERID, required = true) String userID,
            @RequestHeader(value = APPLICATION_MODE, required = true) String applicationMode,
            @RequestHeader Map<String, String> headers) {

        LOGGER.debug("Received POST request. Payload length: {}", 
                requestBody != null ? requestBody.length() : 0);

        // Validate headers
        HeaderValidator.ValidationResult validationResult = headerValidator.validate(
                origin, destination, path, operation, userID, applicationMode);
        if (!validationResult.isValid()) {
            LOGGER.warn("Header validation failed: {}", validationResult.getErrorMessage());
            return responseBuilder.buildErrorResponse(
                    HttpStatus.BAD_REQUEST, "Invalid header", validationResult.getErrorMessage());
        }

        try {
            // Parse JSON payload
            JsonNode jsonNode = jsonParser.parse(requestBody);
            String jsonPayload = jsonParser.toJsonString(jsonNode);

            // Build headers DTO with traceability information
            RequestHeadersDTO headersDTO = headersBuilder.build(
                    origin, destination, path, operation, userID, applicationMode, headers);

            // Send message to RabbitMQ
            messageService.sendMessageWithHead(jsonPayload, headersDTO);

            LOGGER.info("Payload successfully forwarded. Origin: {}, Destination: {}, CorrelationID: {}", 
                    origin, destination, headersDTO.getCorrelationID());

            // Build and return success response
            return responseBuilder.buildSuccessResponse(
                    headersDTO.getCorrelationID(),
                    headersDTO.getTimestamp(),
                    origin,
                    destination,
                    path,
                    operation,
                    applicationMode);

        } catch (RabbitMQException e) {
            LOGGER.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
            return responseBuilder.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Message processing error", 
                    "Failed to process message: " + e.getMessage());
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to process JSON: {}", e.getMessage(), e);
            return responseBuilder.buildErrorResponse(
                    HttpStatus.BAD_REQUEST, 
                    "JSON conversion error", 
                    "Failed to process JSON payload: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing request: {}", e.getMessage(), e);
            return responseBuilder.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Unexpected error", 
                    "An unexpected error occurred while processing the request");
        }
    }
}

