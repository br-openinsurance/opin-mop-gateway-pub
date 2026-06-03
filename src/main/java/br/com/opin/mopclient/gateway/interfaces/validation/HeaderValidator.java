package br.com.opin.mopclient.gateway.interfaces.validation;

import br.com.opin.mopclient.gateway.interfaces.enums.HttpMethod;
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

    /**
     * Validates required headers for anonymization requests.
     * correlationId is independent and mandatory (informed by the user in header X-Correlation-Id).
     *
     * @param correlationId  Correlation ID from header X-Correlation-Id (required, non-empty, min 1 character)
     * @param origin         Origin header value (must be "client" or "server")
     * @param path           Path header value
     * @param operation      Operation header value (must be a valid HTTP method)
     * @param step           Step of the flow in the trace (optional; may be null or blank)
     * @param dataEventoStep Timestamp of the step event (optional; may be null or blank)
     * @param clientSSId     Client SS identifier header value (required)
     * @param serverASId     Server AS identifier header value (required)
     * @return ValidationResult with error message if validation fails
     */
    public ValidationResult validate(String correlationId, String origin, String path,
                                     String operation,
                                     String step, String dataEventoStep,
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
        if (!StringUtils.hasText(clientSSId)) {
            return ValidationResult.error("Header 'clientSSId' must not be empty");
        }
        if (!StringUtils.hasText(serverASId)) {
            return ValidationResult.error("Header 'serverASId' must not be empty");
        }
        return ValidationResult.success();
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

