package br.com.opin.mopclient.gateway.interfaces.validation;

import br.com.opin.mopclient.gateway.interfaces.enums.HttpMethod;
import br.com.opin.mopclient.gateway.interfaces.enums.HttpType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates HTTP request headers for anonymization requests.
 * <p>
 * This component is responsible for validating all required headers
 * and ensuring they meet the business rules.
 */
@Component
public class HeaderValidator {

    private static final String CLIENT = "client";
    private static final String SERVER = "server";

    private static final int CORRELATION_ID_MIN_LENGTH = 1;
    private static final int STATUS_CODE_MIN = 100;
    private static final int STATUS_CODE_MAX = 599;

    /**
     * Validates required headers for anonymization requests.
     * correlationId is independent and mandatory (informed by the user in header X-Correlation-Id).
     *
     * @param correlationId  Correlation ID from header X-Correlation-Id (required, non-empty, min 1 character)
     * @param origin         Origin header value (must be "client" or "server")
     * @param path           Path header value
     * @param operation      Operation header value (must be a valid HTTP method)
     * @param httpType         HTTP message type (required: Request or Response)
     * @param statusCode       HTTP status code (required when httpType is Response; optional when Request)
     * @param clientSSId     Client SS identifier header value (optional; may be null or blank)
     * @param serverASId     Server AS identifier header value (optional; may be null or blank)
     * @return ValidationResult with error message if validation fails
     */
    public ValidationResult validate(String correlationId, String origin, String path,
                                     String operation,
                                     String httpType, String statusCode,
                                     String clientSSId, String serverASId) {
        if (correlationId == null) {
            return ValidationResult.error("Header 'X-Correlation-Id' (correlationId) is required and must not be null");
        }
        if (!StringUtils.hasText(correlationId)) {
            return ValidationResult.error("Header 'X-Correlation-Id' (correlationId) must not be empty");
        }
        String trimmed = correlationId.trim();
        if (trimmed.length() < CORRELATION_ID_MIN_LENGTH) {
            return ValidationResult.error(
                    "Header 'X-Correlation-Id' (correlationId) must be at least " + CORRELATION_ID_MIN_LENGTH + " character(s)");
        }
        if (!StringUtils.hasText(origin)) {
            return ValidationResult.error("Header 'origin' must not be empty");
        }
        if (!CLIENT.equalsIgnoreCase(origin) && !SERVER.equalsIgnoreCase(origin)) {
            return ValidationResult.error("Header 'origin' must be either 'client' or 'server'");
        }
        if (!StringUtils.hasText(path)) {
            return ValidationResult.error("Header 'path' must not be empty");
        }
        if (!StringUtils.hasText(operation)) {
            return ValidationResult.error("Header 'operation' must not be empty");
        }
        if (!HttpMethod.isValid(operation)) {
            return ValidationResult.error(
                    String.format("Header 'operation' must be one of the following values: %s. Received: '%s'",
                            HttpMethod.getValidValues(), operation));
        }
        if (!HttpType.isValid(httpType)) {
            String received = (httpType == null || httpType.isBlank()) ? "" : httpType.trim();
            return ValidationResult.error(
                    String.format("Header 'httpType' must be one of the following values: %s. Received: '%s'",
                            HttpType.getValidValues(), received));
        }
        if (HttpType.isResponse(httpType)) {
            if (!StringUtils.hasText(statusCode)) {
                return ValidationResult.error(
                        "Header 'statusCode' is required when 'httpType' is 'Response'");
            }
            if (!isValidStatusCode(statusCode)) {
                return ValidationResult.error(
                        String.format(
                                "Header 'statusCode' must be a valid HTTP status code (%d-%d). Received: '%s'",
                                STATUS_CODE_MIN, STATUS_CODE_MAX, statusCode));
            }
        } else if (StringUtils.hasText(statusCode) && !isValidStatusCode(statusCode)) {
            return ValidationResult.error(
                    String.format(
                            "Header 'statusCode' must be a valid HTTP status code (%d-%d). Received: '%s'",
                            STATUS_CODE_MIN, STATUS_CODE_MAX, statusCode));
        }
        return ValidationResult.success();
    }

    private static boolean isValidStatusCode(String statusCode) {
        if (!StringUtils.hasText(statusCode)) {
            return false;
        }
        try {
            int code = Integer.parseInt(statusCode.trim());
            return code >= STATUS_CODE_MIN && code <= STATUS_CODE_MAX;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Result of header validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
