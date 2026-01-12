package br.com.opin.mopclient.gateway.shared.dto;

import java.util.Collections;
import java.util.Map;

/**
 * Data Transfer Object representing all headers used in RabbitMQ messages.
 * <p>
 * This DTO encapsulates all header values with their default values,
 * providing a clean and type-safe way to work with message headers.
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * MessageHeadersDTO headers = MessageHeadersDTO.builder()
 *     .origin("service-a")
 *     .destination("service-b")
 *     .correlationId("12345")
 *     .build();
 * }</pre>
 *
 * @author MOP Team
 * @since 1.0
 */
public final class MessageHeadersDTO {

    private final String origin;
    private final String destination;
    private final String path;
    private final String operation;
    private final String correlationId;
    private final String userID;
    private final String applicationMode;
    private final String timestamp;
    private final Map<String, String> customHeaders;

    private MessageHeadersDTO(Builder builder) {
        this.origin = builder.origin;
        this.destination = builder.destination;
        this.path = builder.path;
        this.operation = builder.operation;
        this.correlationId = builder.correlationId;
        this.userID = builder.userID;
        this.applicationMode = builder.applicationMode;
        this.timestamp = builder.timestamp;
        this.customHeaders = builder.customHeaders != null 
                ? Collections.unmodifiableMap(builder.customHeaders) 
                : Collections.emptyMap();
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getPath() {
        return path;
    }

    public String getOperation() {
        return operation;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getUserID() {
        return userID;
    }

    public String getApplicationMode() {
        return applicationMode;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    /**
     * Creates a new builder for MessageHeadersDTO.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating MessageHeadersDTO instances.
     */
    public static final class Builder {
        private String origin;
        private String destination;
        private String path;
        private String operation;
        private String correlationId;
        private String userID;
        private String applicationMode;
        private String timestamp;
        private Map<String, String> customHeaders;

        private Builder() {
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder userID(String userID) {
            this.userID = userID;
            return this;
        }

        public Builder applicationMode(String applicationMode) {
            this.applicationMode = applicationMode;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public MessageHeadersDTO build() {
            return new MessageHeadersDTO(this);
        }
    }

    @Override
    public String toString() {
        return "MessageHeadersDTO{" +
                "origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", path='" + path + '\'' +
                ", operation='" + operation + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", userID='" + userID + '\'' +
                ", applicationMode='" + applicationMode + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", customHeaders=" + customHeaders +
                '}';
    }
}

