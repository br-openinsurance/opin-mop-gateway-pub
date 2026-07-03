package br.com.opin.mopclient.gateway.interfaces.validation;

import br.com.opin.mopclient.gateway.interfaces.enums.HttpMethod;
import br.com.opin.mopclient.gateway.interfaces.enums.HttpType;
import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
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
     * @param origin         Origin header value (must be "client" or "server"; must match httpType)
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
        ValidationResult pathFormatResult = validatePathHeaderFormat(path, operation);
        if (!pathFormatResult.isValid()) {
            return pathFormatResult;
        }
        ValidationResult mopPathResult = validateOpenInsuranceMopPath(path);
        if (!mopPathResult.isValid()) {
            return mopPathResult;
        }
        String normalizedOperation = operation != null ? operation.trim() : operation;
        if (!StringUtils.hasText(normalizedOperation)) {
            return ValidationResult.error("Header 'operation' must not be empty");
        }
        if (!HttpMethod.isValid(normalizedOperation)) {
            return ValidationResult.error(
                    String.format("Header 'operation' must be one of the following values: %s. Received: '%s'",
                            HttpMethod.getValidValues(), normalizedOperation));
        }
        if (!HttpType.isValid(httpType)) {
            String received = (httpType == null || httpType.isBlank()) ? "" : httpType.trim();
            return ValidationResult.error(
                    String.format("Header 'httpType' must be one of the following values: %s. Received: '%s'",
                            HttpType.getValidValues(), received));
        }
        ValidationResult originHttpTypeResult = validateOriginHttpTypeConsistency(origin, httpType);
        if (!originHttpTypeResult.isValid()) {
            return originHttpTypeResult;
        }
        if (HttpType.isResponse(httpType)) {
            if (!StringUtils.hasText(statusCode)) {
                return ValidationResult.error(
                        "Header 'statusCode' is required when 'httpType' is 'response'");
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

    /**
     * {@code path} must not embed the HTTP verb; when present, it must match {@code operation}.
     */
    private static ValidationResult validatePathHeaderFormat(String path, String operation) {
        var embeddedMethod = OpenApiPathMatcher.extractLeadingHttpMethod(path);
        if (embeddedMethod.isEmpty()) {
            return ValidationResult.success();
        }
        String verb = embeddedMethod.get();
        String normalizedOperation = operation != null ? operation.trim() : "";
        if (!StringUtils.hasText(normalizedOperation)) {
            return ValidationResult.error(
                    "Header 'path' must not include the HTTP method ('" + verb + "'). "
                            + "Use header 'operation' for the verb and 'path' only for the resource path "
                            + "(e.g. path: /open-insurance/consents/v3/consents, operation: POST)");
        }
        if (!verb.equalsIgnoreCase(normalizedOperation)) {
            return ValidationResult.error(
                    "Header 'path' embeds HTTP method '" + verb + "' but header 'operation' is '"
                            + normalizedOperation + "'. Remove the verb from 'path' and keep only 'operation'.");
        }
        return ValidationResult.error(
                "Header 'path' must not include the HTTP method ('" + verb + "'). "
                        + "Send path: /open-insurance/consents/v3/consents and operation: " + verb);
    }

    /**
     * {@code path} must normalize to a full MOP path under {@code /open-insurance/}, not only {@code /consents}.
     */
    private static ValidationResult validateOpenInsuranceMopPath(String path) {
        String mopPath = OpenApiPathMatcher.extractOpenInsurancePath(path);
        if (mopPath != null && mopPath.startsWith(OpenApiPathMatcher.OPEN_INSURANCE_PREFIX)) {
            return ValidationResult.success();
        }
        String effective = mopPath != null ? mopPath : path.trim();
        return ValidationResult.error(
                "Header 'path' must be the full Open Insurance path starting with /open-insurance/ "
                        + "(e.g. /open-insurance/consents/v3/consents). "
                        + "Do not send only the operation segment such as /consents. "
                        + "Effective path after normalization: '" + effective + "'");
    }

    /**
     * {@code origin=client} exige {@code httpType=Request}; {@code origin=server} exige {@code httpType=Response}.
     */
    private static ValidationResult validateOriginHttpTypeConsistency(String origin, String httpType) {
        if (CLIENT.equalsIgnoreCase(origin) && HttpType.isResponse(httpType)) {
            return ValidationResult.error(
                    "Header 'httpType' must be 'request' when 'origin' is 'client'");
        }
        if (SERVER.equalsIgnoreCase(origin) && !HttpType.isResponse(httpType)) {
            return ValidationResult.error(
                    "Header 'httpType' must be 'response' when 'origin' is 'server'");
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
