package br.com.opin.mopclient.anonymization.shared.exception.domain;

/**
 * Business exception for domain errors.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
