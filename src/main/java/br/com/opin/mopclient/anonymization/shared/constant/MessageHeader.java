package br.com.opin.mopclient.anonymization.shared.constant;

/**
 * Constants for message headers.
 */
public final class MessageHeader {

    public static final String ORIGIN = "origin";
    public static final String RESPONSE = "response";
    public static final String OPERATION = "operation";
    public static final String PATH = "path";
    public static final String METHOD = "method";
    public static final String TIMESTAMP = "timestamp";
    public static final String HEADERS = "headers";

    private MessageHeader() {
        // Utility class - prevent instantiation
    }
}
