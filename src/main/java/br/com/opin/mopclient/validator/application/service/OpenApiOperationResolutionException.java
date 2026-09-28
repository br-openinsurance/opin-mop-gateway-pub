package br.com.opin.mopclient.validator.application.service;

/**
 * Raised when the MOP path or HTTP method cannot be resolved against an OpenAPI spec.
 */
public final class OpenApiOperationResolutionException extends RuntimeException {

    public static final String CODE_NOT_FOUND = "NOT_FOUND";

    private final String validationCode;

    public OpenApiOperationResolutionException(String message) {
        this(message, null);
    }

    public OpenApiOperationResolutionException(String message, String validationCode) {
        super(message);
        this.validationCode = validationCode;
    }

    public String getValidationCode() {
        return validationCode;
    }
}
