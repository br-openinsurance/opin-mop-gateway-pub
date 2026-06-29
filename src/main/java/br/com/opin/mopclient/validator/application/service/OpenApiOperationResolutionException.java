package br.com.opin.mopclient.validator.application.service;

/**
 * Raised when the MOP path or HTTP method cannot be resolved against an OpenAPI spec.
 */
final class OpenApiOperationResolutionException extends RuntimeException {

    static final String CODE_NOT_FOUND = "NOT_FOUND";

    private final String validationCode;

    OpenApiOperationResolutionException(String message) {
        this(message, null);
    }

    OpenApiOperationResolutionException(String message, String validationCode) {
        super(message);
        this.validationCode = validationCode;
    }

    String getValidationCode() {
        return validationCode;
    }
}
