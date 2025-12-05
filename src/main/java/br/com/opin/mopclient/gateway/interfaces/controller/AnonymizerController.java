package br.com.opin.mopclient.gateway.interfaces.controller;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.*;

import br.com.opin.mopclient.gateway.application.usecase.DataAnonymizerUseCase;
import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

/**
 * Controller responsible for receiving anonymization requests and forwarding them to the use case.
 * <p>
 * This controller validates headers, parses JSON payloads, and handles errors appropriately.
 */
@RestController
@RequestMapping("/data")
public class AnonymizerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymizerController.class);
    
    private final DataAnonymizerUseCase dataAnonymizerUseCase;
    private final ObjectMapper objectMapper;

    public AnonymizerController(
            DataAnonymizerUseCase dataAnonymizerUseCase, 
            ObjectMapper objectMapper) {
        this.dataAnonymizerUseCase = Objects.requireNonNull(
                dataAnonymizerUseCase, 
                "DataAnonymizerUseCase cannot be null");
        this.objectMapper = Objects.requireNonNull(
                objectMapper, 
                "ObjectMapper cannot be null");
    }

    /**
     * Receives a JSON payload via POST and forwards it to the anonymizer use case.
     * <p>
     * Validates required headers and JSON payload before processing.
     *
     * @param requestBody JSON string received in the request body.
     * @param origin Origin header value.
     * @param destination Destination header value.
     * @param path Path header value.
     * @param operation Operation header value.
     * @param certificate Certificate header value.
     * @param userID User ID header value.
     * @param headers All request headers.
     * @return ResponseEntity containing success or error details.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO> receivedRequest(
            @RequestBody String requestBody,
            @RequestHeader(value = ORIGIN, required = true) String origin,
            @RequestHeader(value = DESTINATION, required = true) String destination,
            @RequestHeader(value = PATH, required = true) String path,
            @RequestHeader(value = OPERATION, required = true) String operation,
            @RequestHeader(value = CERTIFCATE, required = true) String certificate,
            @RequestHeader(value = USERID, required = true) String userID,
            @RequestHeader Map<String, String> headers) {

        LOGGER.debug("Received POST request. Payload length: {}", 
                requestBody != null ? requestBody.length() : 0);

        // Validate headers
        String headerValidationError = validateRequiredHeaders(origin, destination, path, operation, userID);
        if (headerValidationError != null) {
            LOGGER.warn("Header validation failed: {}", headerValidationError);
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid header", headerValidationError);
        }

        // Validate and parse JSON
        JsonNode jsonNode = parseAndValidateJson(requestBody);
        if (jsonNode == null) {
            LOGGER.warn("Invalid or empty JSON payload received");
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST, 
                    "Invalid JSON", 
                    "Invalid or empty JSON payload");
        }

        try {
            // Build headers DTO
            RequestHeadersDTO headersDTO = buildRequestHeadersDTO(
                    origin, destination, path, operation, certificate, userID, headers);

            // Ensure correlation ID from header is set in MDC (if not already set by filter)
            // If not present in header, CorrelationIdContext will generate a new one
            String correlationIdFromHeader = headers.get(CORRELATIONID);
            CorrelationIdContext.setCorrelationId(correlationIdFromHeader);

            // Convert JSON to string (using writeValueAsString for better performance)
            String jsonPayload = convertJsonNodeToString(jsonNode);

            // Send message to use case
            dataAnonymizerUseCase.sendMessageWithHead(jsonPayload, headersDTO);

            LOGGER.info("Payload successfully forwarded to anonymizer. Origin: {}, Destination: {}", 
                    origin, destination);

            return buildSuccessResponse("Message sent successfully");

        } catch (RabbitMQException e) {
            LOGGER.error("Failed to send message to RabbitMQ. Reason: {}", e.getMessage(), e);
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Message processing error",
                    "Failed to process message: " + e.getMessage());
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert JSON. Reason: {}", e.getMessage(), e);
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "JSON conversion error",
                    "Failed to process JSON payload: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing request. Reason: {}", e.getMessage(), e);
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error",
                    "An unexpected error occurred while processing the request");
        }
    }

    /**
     * Validates that all required headers are present and not empty.
     *
     * @param origin Origin header value.
     * @param destination Destination header value.
     * @param path Path header value.
     * @param operation Operation header value.
     * @param userID User ID header value.
     * @return Error message if validation fails, null otherwise.
     */
    private String validateRequiredHeaders(
            String origin, String destination, String path, String operation, String userID) {
        
        if (!StringUtils.hasText(origin)) {
            return "Header 'origin' must not be empty";
        }
        if (!StringUtils.hasText(destination)) {
            return "Header 'destination' must not be empty";
        }
        if (!StringUtils.hasText(path)) {
            return "Header 'path' must not be empty";
        }
        if (!StringUtils.hasText(operation)) {
            return "Header 'operation' must not be empty";
        }
        if (!StringUtils.hasText(userID)) {
            return "Header 'userID' must not be empty";
        }
        return null;
    }

    /**
     * Parses and validates the JSON request body.
     *
     * @param requestBody The JSON string to parse.
     * @return JsonNode if valid, null otherwise.
     */
    private JsonNode parseAndValidateJson(String requestBody) {
        if (!StringUtils.hasText(requestBody)) {
            LOGGER.warn("Received null or empty request body");
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            if (jsonNode == null || jsonNode.isEmpty()) {
                LOGGER.warn("Parsed JSON is null or empty");
                return null;
            }
            return jsonNode;
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse JSON. Reason: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts JsonNode to string using ObjectMapper for better performance.
     *
     * @param jsonNode The JsonNode to convert.
     * @return JSON string representation.
     * @throws JsonProcessingException if conversion fails.
     */
    private String convertJsonNodeToString(JsonNode jsonNode) throws JsonProcessingException {
        return objectMapper.writeValueAsString(jsonNode);
    }

    /**
     * Builds RequestHeadersDTO from header values.
     *
     * @param origin Origin header value.
     * @param destination Destination header value.
     * @param path Path header value.
     * @param operation Operation header value.
     * @param certificate Certificate header value.
     * @param userID User ID header value.
     * @param headers All request headers.
     * @return RequestHeadersDTO instance.
     */
    private RequestHeadersDTO buildRequestHeadersDTO(
            String origin, String destination, String path, String operation,
            String certificate, String userID, Map<String, String> headers) {
        
        return RequestHeadersDTO.builder()
                .origin(origin)
                .destination(destination)
                .path(path)
                .operation(operation)
                .certificate(certificate)
                .userID(userID)
                .headers(headers)
                .build();
    }

    /**
     * Builds a success response.
     *
     * @param message Success message.
     * @return ResponseEntity with success response.
     */
    private ResponseEntity<ApiResponseDTO> buildSuccessResponse(String message) {
        ApiResponseDTO response = ApiResponseDTO.builder()
                .message(message)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Builds an error response.
     *
     * @param status HTTP status code.
     * @param error Error type.
     * @param details Error details.
     * @return ResponseEntity with error response.
     */
    private ResponseEntity<ApiResponseDTO> buildErrorResponse(
            HttpStatus status, String error, String details) {
        
        ApiResponseDTO response = ApiResponseDTO.builder()
                .error(error)
                .details(details)
                .build();
        return ResponseEntity.status(status.value()).body(response);
    }
}
