package br.com.opin.mopclient.gateway.interfaces.validation;

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

    private static final String TRANSMITTER = "TRANSMITTER";
    private static final String RECEIVER = "RECEIVER";

    /**
     * Validates required headers for anonymization requests.
     *
     * @param origin          Origin header value
     * @param destination     Destination header value
     * @param path            Path header value
     * @param operation       Operation header value
     * @param userID          UserID header value
     * @param applicationMode ApplicationMode header value
     * @return ValidationResult with error message if validation fails, null otherwise
     */
    public ValidationResult validate(String origin, String destination, String path,
                                     String operation, String userID, String applicationMode) {
        if (!StringUtils.hasText(origin)) {
            return ValidationResult.error("Header 'origin' must not be empty");
        }
        if (!StringUtils.hasText(destination)) {
            return ValidationResult.error("Header 'destination' must not be empty");
        }
        if (!StringUtils.hasText(path)) {
            return ValidationResult.error("Header 'path' must not be empty");
        }
        if (!StringUtils.hasText(operation)) {
            return ValidationResult.error("Header 'operation' must not be empty");
        }
        if (!StringUtils.hasText(userID)) {
            return ValidationResult.error("Header 'userID' must not be empty");
        }
        if (!StringUtils.hasText(applicationMode)) {
            return ValidationResult.error("Header 'applicationMode' must not be empty");
        }
        if (!TRANSMITTER.equalsIgnoreCase(applicationMode) && !RECEIVER.equalsIgnoreCase(applicationMode)) {
            return ValidationResult.error("Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'");
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

