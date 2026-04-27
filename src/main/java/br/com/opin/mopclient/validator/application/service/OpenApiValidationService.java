package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.interfaces.dto.ValidationResultDTO;
import br.com.opin.mopclient.validator.interfaces.dto.ValidationViolationDTO;
import br.com.opin.mopclient.validator.interfaces.dto.ValidationResponseDTO;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.validation.RequestValidator;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service responsible for validating requests against the OpenAPI specification.
 * Uses the openapi4j library to validate payloads, headers, and endpoints.
 * Implements restrictive validations and fault-tolerant error handling.
 */
@Service
public class OpenApiValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiValidationService.class);
    private static final int MAX_VALIDATION_ERRORS = 100; // Maximum errors to process
    
    private final OpenApi3 openApi;

    /**
     * Constructor for the OpenAPI validation service.
     *
     * @param openApi The OpenAPI specification to be used for validation
     * @throws NullPointerException If openApi is null
     */
    public OpenApiValidationService(OpenApi3 openApi) {
        this.openApi = Objects.requireNonNull(openApi, "OpenApi3 cannot be null");
    }

    /**
     * Validates a payload against the OpenAPI specification.
     * Implements restrictive validations and fault-tolerant error handling.
     *
     * @param rawPayload  The JSON payload to be validated (cannot be null or empty)
     * @param headers     The HTTP headers of the request (can be null)
     * @param endpointUrl The URL of the endpoint to be validated (cannot be null or empty)
     * @return ValidationResponseDTO containing the validation result (success or errors)
     */
    public ValidationResponseDTO validate(String rawPayload, HttpHeaders headers, String endpointUrl) {
        // Restrictive input validations
        validateInputs(rawPayload, endpointUrl);

        String normalizedEndpointUrl = normalizeEndpointUrl(endpointUrl);
        
        long startTime = System.currentTimeMillis();
        logger.info("Starting validation for endpoint: {}", normalizedEndpointUrl);
        logger.debug("Payload received (size: {} bytes)", rawPayload != null ? rawPayload.length() : 0);
        logger.debug("Request headers: {}", headers);

        ValidationResponseDTO response = new ValidationResponseDTO();
        
        try {
            // Payload normalization and validation
            String normalizedPayload = normalizePayload(rawPayload);
            Body requestBody = createRequestBody(normalizedPayload);
            
            // OpenAPI request construction
            Request openApiRequest = buildOpenApiRequest(normalizedEndpointUrl, headers, requestBody);
            
            // Validation
            new RequestValidator(openApi).validate(openApiRequest);
            
            logger.info("Validation successful for endpoint: {}", normalizedEndpointUrl);
            response.setValidationResult(buildSuccessResponse());
            
        } catch (ValidationException e) {
            logger.warn("Validation failed for endpoint: {}. Reason: {}", normalizedEndpointUrl, e.getMessage());
            if (e.results() != null && e.results().items() != null) {
                logger.debug("Validation error details: {}", e.results().items());
            } else {
                logger.debug("Validation error details: results() is null");
            }
            response.setValidationResult(buildValidationErrorResponse(e));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Input validation error: {}", e.getMessage());
            response.setValidationResult(buildInputValidationErrorResponse(e));
            
        } catch (Exception ex) {
            logger.error("Unexpected error during validation for endpoint: {}", endpointUrl, ex);
            // Fault-tolerant: returns error instead of throwing exception
            response.setValidationResult(buildInternalErrorResponse(ex));
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Validation process completed in {} ms", duration);

        return response;
    }

    /**
     * Validates input parameters.
     *
     * @param rawPayload  The payload to be validated
     * @param endpointUrl The endpoint URL to be validated
     * @throws IllegalArgumentException If any parameter is invalid
     */
    private void validateInputs(String rawPayload, String endpointUrl) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalArgumentException("Endpoint URL cannot be null or empty");
        }
    }

    /**
     * Normalizes the payload by removing unnecessary whitespace.
     * Fault-tolerant: returns original payload if normalization fails.
     *
     * @param payload The payload to be normalized
     * @return The normalized payload
     */
    private String normalizePayload(String payload) {
        try {
            if (payload == null || payload.isBlank()) {
                return payload;
            }
            return payload.trim();
        } catch (Exception e) {
            logger.warn("Error normalizing payload, using original value: {}", e.getMessage());
            return payload;
        }
    }

    /**
     * Normalizes the endpoint URL/path used for OpenAPI validation.
     * <p>
     * The OpenAPI spec typically defines paths without a trailing slash. Some callers send
     * values like {@code /resource/}. This method removes trailing slashes (except root),
     * and if a full URL is provided, it extracts only the path component.
     */
    private String normalizeEndpointUrl(String endpointUrl) {
        if (endpointUrl == null) {
            return null;
        }

        String value = endpointUrl.trim();
        if (value.isEmpty()) {
            return value;
        }

        // If a full URL is accidentally provided, validate against its path.
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                URI uri = URI.create(value);
                if (uri.getPath() != null && !uri.getPath().isBlank()) {
                    value = uri.getPath();
                }
            }
        } catch (Exception e) {
            // Ignore parsing issues and keep the original value.
        }

        // Ensure leading slash for openapi4j routing.
        if (!value.startsWith("/")) {
            value = "/" + value;
        }

        // Remove trailing slashes (except when it's just "/").
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    /**
     * Creates the request Body from the payload.
     * Fault-tolerant: throws descriptive exception if it fails.
     *
     * @param payload The normalized payload
     * @return The created Body
     * @throws IllegalArgumentException If the payload cannot be converted to Body
     */
    private Body createRequestBody(String payload) {
        try {
            return Body.from(payload);
        } catch (Exception e) {
            logger.error("Error creating Body from payload: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid payload: cannot create request body", e);
        }
    }

    /**
     * Builds an OpenAPI request from the provided parameters.
     * Fault-tolerant: gracefully handles null or invalid headers.
     *
     * @param endpointUrl The endpoint URL
     * @param headers      The HTTP headers
     * @param requestBody The request body
     * @return A configured Request instance
     */
    private Request buildOpenApiRequest(String endpointUrl, HttpHeaders headers, Body requestBody) {
        try {
            Map<String, Collection<String>> headersMap = normalizeHeaders(headers);
            
            return new DefaultRequest.Builder(endpointUrl, Request.Method.POST)
                    .body(requestBody)
                    .headers(headersMap)
                    .build();
        } catch (Exception e) {
            logger.error("Error building OpenAPI request: {}", e.getMessage(), e);
            // Fault-tolerant: tries to build without headers if there's an error
            return new DefaultRequest.Builder(endpointUrl, Request.Method.POST)
                    .body(requestBody)
                    .headers(new HashMap<>())
                    .build();
        }
    }

    /**
     * Normalizes HTTP headers to the format expected by OpenAPI.
     * Fault-tolerant: returns empty map if there's an error.
     *
     * @param headers The HTTP headers
     * @return Normalized headers map
     */
    private Map<String, Collection<String>> normalizeHeaders(HttpHeaders headers) {
        Map<String, Collection<String>> headersMap = new HashMap<>();
        
        try {
            if (headers != null) {
                headers.forEach((key, values) -> {
                    if (key != null && !key.isBlank() && values != null && !values.isEmpty()) {
                        // Filters null or empty values
                        List<String> validValues = new ArrayList<>();
                        values.forEach(value -> {
                            if (value != null && !value.isBlank()) {
                                validValues.add(value);
                            }
                        });
                        if (!validValues.isEmpty()) {
                            headersMap.put(key, validValues);
                        }
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("Error normalizing headers, using empty map: {}", e.getMessage());
            return new HashMap<>();
        }
        
        return headersMap;
    }

    /**
     * Builds a success response.
     *
     * @return ValidationResultDTO with OK status
     */
    private ValidationResultDTO buildSuccessResponse() {
        return ValidationResultDTO.builder()
                .detailMessage("Validation completed successfully")
                .httpStatusCode(HttpStatus.OK)
                .build();
    }

    /**
     * Builds a validation error response.
     * Fault-tolerant: limits the number of errors processed.
     *
     * @param validationException The validation exception containing the errors
     * @return ValidationResultDTO with validation error details
     */
    private ValidationResultDTO buildValidationErrorResponse(ValidationException validationException) {
        List<ValidationViolationDTO> errorDetails = new ArrayList<>();
        
        try {
            // Fault-tolerant: check if results() is not null before accessing items()
            var validationResults = validationException.results();
            if (validationResults != null && validationResults.items() != null) {
                int errorCount = 0;
                for (var item : validationResults.items()) {
                    if (errorCount >= MAX_VALIDATION_ERRORS) {
                        logger.warn("Maximum of {} validation errors reached, stopping processing", MAX_VALIDATION_ERRORS);
                        break;
                    }
                    
                    try {
                        logger.debug("Validation error - Code: {}, Message: {}, Severity: {}, Crumbs: {}, SchemaCrumbs: {}",
                                item.code(), item.message(), item.severity(), item.dataCrumbs(), item.schemaCrumbs());
                        
                        ValidationViolationDTO error = ValidationViolationDTO.builder()
                                .code(item.code())
                                .message(sanitizeMessage(item.message()))
                                .severity(item.severity() != null ? item.severity().name() : "UNKNOWN")
                                .crumbs(sanitizeString(item.dataCrumbs()))
                                .build();
                        errorDetails.add(error);
                        errorCount++;
                    } catch (Exception e) {
                        logger.warn("Error processing validation item: {}", e.getMessage());
                        // Fault-tolerant: continues processing other errors
                    }
                }
            } else {
                logger.warn("ValidationException results() is null, creating generic error");
                // Fault-tolerant: creates a generic error when results are null
                errorDetails.add(ValidationViolationDTO.builder()
                        .code(0)
                        .message("Validation failed: " + sanitizeMessage(validationException.getMessage()))
                        .severity("ERROR")
                        .crumbs("")
                        .build());
            }
        } catch (Exception e) {
            logger.error("Error processing validation errors: {}", e.getMessage(), e);
            // Fault-tolerant: returns at least a generic message
            errorDetails.add(ValidationViolationDTO.builder()
                    .code(0)
                    .message("Error processing validation details: " + sanitizeMessage(e.getMessage()))
                    .severity("ERROR")
                    .crumbs("")
                    .build());
        }

        String detailMessage = validationException.getMessage() != null 
                ? "Validation failed: " + sanitizeMessage(validationException.getMessage())
                : "Validation failed";

        return ValidationResultDTO.builder()
                .detailMessage(detailMessage)
                .validations(errorDetails)
                .httpStatusCode(HttpStatus.BAD_REQUEST)
                .build();
    }

    /**
     * Builds an input validation error response.
     *
     * @param exception The input validation exception
     * @return ValidationResultDTO with input validation error
     */
    private ValidationResultDTO buildInputValidationErrorResponse(IllegalArgumentException exception) {
        return ValidationResultDTO.builder()
                .httpStatusCode(HttpStatus.BAD_REQUEST)
                .detailMessage("Input validation error: " + sanitizeMessage(exception.getMessage()))
                .build();
    }

    /**
     * Builds an internal error response.
     * Fault-tolerant: handles null exceptions or exceptions without messages.
     *
     * @param exception The exception that caused the error
     * @return ValidationResultDTO with internal server error status
     */
    private ValidationResultDTO buildInternalErrorResponse(Exception exception) {
        String errorMessage = "Internal server error";
        
        try {
            if (exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()) {
                errorMessage = sanitizeMessage(exception.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Error extracting error message: {}", e.getMessage());
        }
                
        return ValidationResultDTO.builder()
                .httpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .detailMessage("Internal server error: " + errorMessage)
                .build();
    }

    /**
     * Sanitizes a message by removing potentially dangerous characters.
     * Fault-tolerant: returns empty string if message is null.
     *
     * @param message The message to be sanitized
     * @return The sanitized message
     */
    private String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        
        try {
            // Removes control characters and limits size
            String sanitized = message.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
            // Shorten "Operation path not found from URL '...'" (WAF false-positive mitigation)
            // (reason: frontdoor was flagging the original message as SQL injection)
            sanitized = sanitized.replaceAll(
                    "Operation path not found from URL '([^']+)'\\.",
                    "path not found from");
            if (sanitized.length() > 1000) {
                sanitized = sanitized.substring(0, 1000) + "...";
            }
            return sanitized;
        } catch (Exception e) {
            logger.warn("Error sanitizing message: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Sanitizes a string by removing potentially dangerous characters.
     *
     * @param str The string to be sanitized
     * @return The sanitized string or empty string if null
     */
    private String sanitizeString(String str) {
        if (str == null) {
            return "";
        }
        return sanitizeMessage(str);
    }
}
