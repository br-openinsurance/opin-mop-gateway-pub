package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.interfaces.dto.ValidationResultDTO;
import br.com.opin.mopclient.validator.interfaces.dto.ValidationViolationDTO;
import br.com.opin.mopclient.validator.interfaces.dto.ValidationResponseDTO;
import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.Response;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.openapi4j.operation.validator.validation.RequestValidator;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates payloads against Open Insurance OpenAPI specifications in {@code swagger/current/}
 * via {@link OpenApiCurrentSpecRegistry} (MOP path = {@code servers.url} path + {@code paths} key).
 * Uses HTTP {@code operation} header and {@code httpType} (Request=request body, Response=response body).
 * {@code origin} must be consistent with {@code httpType} (enforced by {@link br.com.opin.mopclient.gateway.interfaces.validation.HeaderValidator}).
 */
@Service
public class OpenApiValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiValidationService.class);
    private static final int MAX_VALIDATION_ERRORS = 100;

    private final OpenApiCurrentSpecRegistry currentSpecRegistry;

    public OpenApiValidationService(OpenApiCurrentSpecRegistry currentSpecRegistry) {
        this.currentSpecRegistry = Objects.requireNonNull(currentSpecRegistry, "OpenApiCurrentSpecRegistry cannot be null");
    }

    public ValidationResponseDTO validate(String rawPayload, HttpHeaders headers, String endpointUrl) {
        return validate(rawPayload, headers, endpointUrl, null, null, null);
    }

    /**
     * Validates a payload against the resolved OpenAPI operation.
     *
     * @param rawPayload   JSON body
     * @param headers      HTTP headers (OpenAPI parameters)
     * @param endpointUrl  full MOP {@code path} header
     * @param httpMethod   HTTP verb from MOP {@code operation} header (GET, POST, …)
     * @param httpType     {@code Request} validates request body; {@code Response} validates response body
     * @param statusCode   HTTP status for response validation when {@code httpType} is {@code Response}
     */
    public ValidationResponseDTO validate(
            String rawPayload,
            HttpHeaders headers,
            String endpointUrl,
            String httpMethod,
            String httpType,
            String statusCode) {

        validateInputs(rawPayload, endpointUrl);

        String normalizedMopPath = normalizeEndpointUrl(endpointUrl);
        Request.Method method = toRequestMethod(httpMethod);
        boolean validateAsResponse = isResponseHttpType(httpType);

        long startTime = System.currentTimeMillis();
        ValidationResponseDTO response = new ValidationResponseDTO();

        try {
            ResolvedValidationTarget target = resolveValidationTarget(normalizedMopPath);
            logger.info(
                    "Starting validation | mopPath={} | validationPath={} | method={} | httpType={} | spec={}",
                    normalizedMopPath,
                    target.validationPath(),
                    method,
                    httpType != null && !httpType.isBlank() ? httpType : "Request",
                    target.sourceLabel());
            String normalizedPayload = normalizePayload(rawPayload);
            int responseStatus = resolveResponseStatus(statusCode, method);
            normalizedPayload = OpenApiStringNumericPayloadCoercer.coerce(
                    target.openApi(),
                    target.validationPath(),
                    target.relativePath(),
                    method,
                    validateAsResponse,
                    responseStatus,
                    normalizedPayload);
            Map<String, Collection<String>> headersMap = normalizeHeaders(headers);
            RequestValidator validator = new RequestValidator(target.openApi());

            OpenApiOperationSupport.ensureOperationExists(
                    target.openApi(),
                    target.validationPath(),
                    target.relativePath(),
                    method,
                    target.mopPath());

            if (validateAsResponse) {
                Body responseBody = createRequestBody(normalizedPayload);
                Response openApiResponse = new DefaultResponse.Builder(responseStatus)
                        .body(responseBody)
                        .headers(headersMap)
                        .build();
                var responseValidation = OpenApiOperationSupport.validateResponse(
                        target.openApi(), target.validationPath(), method, openApiResponse);
                if (!responseValidation.isValid()) {
                    logger.warn("Validation failed | validationPath={} | httpType=Response", target.validationPath());
                    ValidationResultDTO errorResult = buildValidationErrorResponse(
                            responseValidation.results(), "Invalid response");
                    response.setValidationResult(hasValidationViolations(errorResult)
                            ? errorResult
                            : buildSuccessResponse());
                    logger.info("Validation process completed in {} ms", System.currentTimeMillis() - startTime);
                    return response;
                }
            } else {
                Body requestBody = createRequestBody(normalizedPayload);
                // openapi4j RequestValidator matches server.url + paths key; use full MOP path, not YAML template.
                Request openApiRequest = new DefaultRequest.Builder(target.mopPath(), method)
                        .body(requestBody)
                        .headers(headersMap)
                        .build();
                validator.validate(openApiRequest);
            }

            logger.info("Validation successful | validationPath={} | spec={}", target.validationPath(), target.sourceLabel());
            response.setValidationResult(buildSuccessResponse());

        } catch (OpenApiOperationResolutionException e) {
            logger.warn("Validation failed | mopPath={} | reason={}", normalizedMopPath, e.getMessage());
            response.setValidationResult(buildValidationErrorResponse(
                    null, e.getMessage(), e.getValidationCode()));
        } catch (ValidationException e) {
            logger.warn("Validation failed | mopPath={} | reason={}", normalizedMopPath, e.getMessage());
            ValidationResultDTO errorResult = buildValidationErrorResponse(e);
            response.setValidationResult(hasValidationViolations(errorResult)
                    ? errorResult
                    : buildSuccessResponse());
        } catch (IllegalArgumentException e) {
            logger.warn("Input validation error: {}", e.getMessage());
            response.setValidationResult(buildInputValidationErrorResponse(e));
        } catch (Exception ex) {
            logger.error("Unexpected error during validation | mopPath={}", normalizedMopPath, ex);
            response.setValidationResult(buildInternalErrorResponse(ex));
        }

        logger.info("Validation process completed in {} ms", System.currentTimeMillis() - startTime);
        return response;
    }

    private ResolvedValidationTarget resolveValidationTarget(String normalizedMopPath) {
        Optional<OpenApiSpecResolution> modular = currentSpecRegistry.resolve(normalizedMopPath);
        if (modular.isPresent()) {
            OpenApiSpecResolution resolution = modular.get();
            // OpenAPI specs declare paths with placeholders ({policyId}); openapi4j resolves
            // operations by that template key, not by the concrete MOP path with real IDs.
            return new ResolvedValidationTarget(
                    resolution.openApi(),
                    resolution.pathTemplate(),
                    resolution.relativePath(),
                    normalizedMopPath,
                    resolution.sourceFile());
        }
        throw new OpenApiOperationResolutionException(
                "Operation path not found from URL '" + normalizedMopPath + "'.",
                OpenApiOperationResolutionException.CODE_NOT_FOUND);
    }

    private static Request.Method toRequestMethod(String httpMethod) {
        if (httpMethod == null || httpMethod.isBlank()) {
            return Request.Method.POST;
        }
        return switch (httpMethod.trim().toUpperCase()) {
            case "GET" -> Request.Method.GET;
            case "PUT" -> Request.Method.PUT;
            case "PATCH" -> Request.Method.PATCH;
            case "DELETE" -> Request.Method.DELETE;
            case "HEAD" -> Request.Method.HEAD;
            case "OPTIONS" -> Request.Method.OPTIONS;
            default -> Request.Method.POST;
        };
    }

    private static int inferResponseStatus(Request.Method method) {
        return switch (method) {
            case POST -> 201;
            case DELETE -> 204;
            default -> 200;
        };
    }

    private static int resolveResponseStatus(String statusCode, Request.Method method) {
        if (statusCode != null && !statusCode.isBlank()) {
            try {
                return Integer.parseInt(statusCode.trim());
            } catch (NumberFormatException ignored) {
                // fall through to inferred default
            }
        }
        return inferResponseStatus(method);
    }

    private static boolean isResponseHttpType(String httpType) {
        return httpType != null && httpType.trim().equalsIgnoreCase("Response");
    }

    private void validateInputs(String rawPayload, String endpointUrl) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalArgumentException("Endpoint URL cannot be null or empty");
        }
    }

    private String normalizePayload(String payload) {
        try {
            return payload != null ? payload.trim() : payload;
        } catch (Exception e) {
            logger.warn("Error normalizing payload, using original value: {}", e.getMessage());
            return payload;
        }
    }

    private String normalizeEndpointUrl(String endpointUrl) {
        return OpenApiPathMatcher.extractOpenInsurancePath(endpointUrl);
    }

    private Body createRequestBody(String payload) {
        try {
            return Body.from(payload);
        } catch (Exception e) {
            logger.error("Error creating Body from payload: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid payload: cannot create request body", e);
        }
    }

    private Map<String, Collection<String>> normalizeHeaders(HttpHeaders headers) {
        Map<String, Collection<String>> headersMap = new HashMap<>();
        try {
            if (headers != null) {
                headers.forEach((key, values) -> {
                    if (key != null && !key.isBlank() && values != null && !values.isEmpty()) {
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

    private static boolean hasValidationViolations(ValidationResultDTO result) {
        return result != null
                && result.getValidations() != null
                && !result.getValidations().isEmpty();
    }

    private ValidationResultDTO buildSuccessResponse() {
        return ValidationResultDTO.builder()
                .detailMessage("Validation completed successfully")
                .statusCode(HttpStatus.OK)
                .build();
    }

    private ValidationResultDTO buildValidationErrorResponse(ValidationException validationException) {
        return buildValidationErrorResponse(
                validationException.results(),
                validationException.getMessage());
    }

    private ValidationResultDTO buildValidationErrorResponse(ValidationResults validationResults, String fallbackMessage) {
        return buildValidationErrorResponse(validationResults, fallbackMessage, null);
    }

    private ValidationResultDTO buildValidationErrorResponse(
            ValidationResults validationResults,
            String fallbackMessage,
            String fallbackCode) {
        List<ValidationViolationDTO> errorDetails = new ArrayList<>();
        try {
            if (validationResults != null && validationResults.items() != null) {
                int errorCount = 0;
                for (var item : validationResults.items()) {
                    if (errorCount >= MAX_VALIDATION_ERRORS) {
                        break;
                    }
                    try {
                        if (OpenApiSpecCompatibilityPatcher.isStringNumericFormatFalsePositive(
                                item.code(), item.message())) {
                            logger.debug("Suppressing openapi4j string/numeric format false-positive: {}",
                                    item.message());
                            continue;
                        }
                        errorDetails.add(ValidationViolationDTO.builder()
                                .code(String.valueOf(item.code()))
                                .message(sanitizeMessage(item.message()))
                                .severity(item.severity() != null ? item.severity().name() : "ERROR")
                                .crumbs(sanitizeString(item.dataCrumbs()))
                                .build());
                        errorCount++;
                    } catch (Exception e) {
                        logger.warn("Error processing validation item: {}", e.getMessage());
                    }
                }
            } else {
                errorDetails.add(ValidationViolationDTO.builder()
                        .code(resolveValidationCode(fallbackMessage, fallbackCode))
                        .message(sanitizeMessage(fallbackMessage))
                        .severity("ERROR")
                        .crumbs("")
                        .build());
            }
        } catch (Exception e) {
            errorDetails.add(ValidationViolationDTO.builder()
                    .code("0")
                    .message("Error processing validation details: " + sanitizeMessage(e.getMessage()))
                    .severity("ERROR")
                    .crumbs("")
                    .build());
        }

        String detailMessage = fallbackMessage != null && !fallbackMessage.isBlank()
                ? "Validation failed: " + sanitizeMessage(fallbackMessage)
                : "Validation failed";

        return ValidationResultDTO.builder()
                .detailMessage(detailMessage)
                .validations(errorDetails)
                .statusCode(HttpStatus.BAD_REQUEST)
                .build();
    }

    private ValidationResultDTO buildInputValidationErrorResponse(IllegalArgumentException exception) {
        return ValidationResultDTO.builder()
                .statusCode(HttpStatus.BAD_REQUEST)
                .detailMessage("Input validation error: " + sanitizeMessage(exception.getMessage()))
                .build();
    }

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
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .detailMessage("Internal server error: " + errorMessage)
                .build();
    }

    private static String resolveValidationCode(String fallbackMessage, String fallbackCode) {
        if (fallbackCode != null && !fallbackCode.isBlank()) {
            return fallbackCode;
        }
        return "0";
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        try {
            String sanitized = message.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
            if (sanitized.length() > 1000) {
                sanitized = sanitized.substring(0, 1000) + "...";
            }
            return sanitized;
        } catch (Exception e) {
            return "";
        }
    }

    private String sanitizeString(String str) {
        return str == null ? "" : sanitizeMessage(str);
    }

    private record ResolvedValidationTarget(
            OpenApi3 openApi,
            String validationPath,
            String relativePath,
            String mopPath,
            String sourceLabel) {
    }
}
