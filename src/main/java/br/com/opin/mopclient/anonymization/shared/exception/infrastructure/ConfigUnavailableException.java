package br.com.opin.mopclient.anonymization.shared.exception.infrastructure;

/**
 * Exception thrown when anonymization configuration is unavailable.
 */
public class ConfigUnavailableException extends RuntimeException {

    public ConfigUnavailableException(String message) {
        super(message);
    }

    public ConfigUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
