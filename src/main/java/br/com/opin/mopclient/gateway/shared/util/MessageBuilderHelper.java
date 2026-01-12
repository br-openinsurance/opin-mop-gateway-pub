package br.com.opin.mopclient.gateway.shared.util;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.dto.MessageHeadersDTO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.*;

/**
 * Utility class for building and reading RabbitMQ Message objects with standardized headers.
 * <p>
 * This helper class provides methods to:
 * <ul>
 *   <li>Build Message objects from payload and headers DTO</li>
 *   <li>Extract header values from existing Message objects</li>
 *   <li>Provide default values for missing headers</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * // Building a message
 * RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
 *     .origin("service-a")
 *     .destination("service-b")
 *     .build();
 * 
 * Message message = MessageBuilderHelper.buildMessage("{\"data\":\"value\"}", headersDTO);
 * 
 * // Reading header values from a message
 * MessageHeadersDTO headers = MessageBuilderHelper.extractHeaders(message);
 * String origin = headers.getOrigin();
 * String correlationId = headers.getCorrelationId();
 * }</pre>
 *
 * @author MOP Team
 * @since 1.0
 */
public final class MessageBuilderHelper {

    private MessageBuilderHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final String DEFAULT_ORIGIN = "unknown-origin";
    private static final String DEFAULT_DESTINATION = "unknown-destination";
    private static final String DEFAULT_PATH = "unknown-path";
    private static final String DEFAULT_OPERATION = "unknown-operation";
    private static final String DEFAULT_USER_ID = "unknown-userID";
    private static final String DEFAULT_APPLICATION_MODE = "unknown-applicationMode";
    private static final String SERVICE_PREFIX = "mop-gateway";
    private static final DateTimeFormatter CORRELATION_ID_TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    /**
     * Builds a RabbitMQ Message with standardized headers from a payload and headers DTO.
     * <p>
     * Extracts header values from the DTO, using default values when not provided.
     * Automatically generates correlation ID and timestamp if not present.
     *
     * @param payload the message payload (must not be null or blank)
     * @param headersDTO the headers DTO containing header information (can be null)
     * @return a configured Message object ready to be sent
     * @throws IllegalArgumentException if payload is null or blank
     */
    public static Message buildMessage(String payload, RequestHeadersDTO headersDTO) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload must not be null or blank");
        }

        MessageHeadersDTO headers = createMessageHeadersFromDTO(headersDTO);
        return buildMessageWithHeaders(payload, headers);
    }

    /**
     * Builds a RabbitMQ Message from payload and MessageHeadersDTO object.
     *
     * @param payload the message payload (must not be null or blank)
     * @param headers the MessageHeadersDTO object containing all header values
     * @return a configured Message object ready to be sent
     */
    private static Message buildMessageWithHeaders(String payload, MessageHeadersDTO headers) {
        // Ensures that correlationId and timestamp always have valid values
        String correlationId = headers.getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateTraceableCorrelationId();
        }
        
        String timestamp = headers.getTimestamp();
        if (timestamp == null || timestamp.isBlank()) {
            timestamp = Instant.now().toString();
        }
        
        // Create MessageProperties first to set headers with exact case preservation
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader(ORIGIN, headers.getOrigin());
        properties.setHeader(DESTINATION, headers.getDestination());
        properties.setHeader(PATH, headers.getPath());
        properties.setHeader(HEADERS, headers.getCustomHeaders());
        properties.setHeader(OPERATION, headers.getOperation());
        properties.setHeader(CORRELATIONID, correlationId);
        properties.setHeader(USERID, headers.getUserID());
        properties.setHeader(APPLICATION_MODE, headers.getApplicationMode());
        properties.setHeader(TIMESTAMP, timestamp);
        
        // Build message with body and properties
        return MessageBuilder
                .withBody(payload.getBytes(StandardCharsets.UTF_8))
                .andProperties(properties)
                .build();
    }

    /**
     * Extracts all headers from a RabbitMQ Message object into a MessageHeadersDTO object.
     *
     * @param message the RabbitMQ Message (must not be null)
     * @return a MessageHeadersDTO object containing all extracted header values
     * @throws NullPointerException if message is null
     */
    public static MessageHeadersDTO extractHeaders(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return createMessageHeadersFromMessage(message);
    }

    /**
     * Creates a MessageHeadersDTO from a RequestHeadersDTO, applying default values where needed.
     *
     * @param headersDTO the RequestHeadersDTO (can be null)
     * @return a MessageHeadersDTO with all values set, using defaults when necessary
     */
    public static MessageHeadersDTO createMessageHeadersFromDTO(RequestHeadersDTO headersDTO) {
        if (headersDTO == null) {
            return createMessageHeadersWithDefaults();
        }

        return MessageHeadersDTO.builder()
                .origin(getValueOrDefault(headersDTO.getOrigin(), DEFAULT_ORIGIN))
                .destination(getValueOrDefault(headersDTO.getDestination(), DEFAULT_DESTINATION))
                .path(getValueOrDefault(headersDTO.getPath(), DEFAULT_PATH))
                .operation(getValueOrDefault(headersDTO.getOperation(), DEFAULT_OPERATION))
                .correlationId(getValueOrDefault(headersDTO.getCorrelationID(), generateTraceableCorrelationId()))
                .userID(getValueOrDefault(headersDTO.getUserID(), DEFAULT_USER_ID))
                .applicationMode(getValueOrDefault(headersDTO.getApplicationMode(), DEFAULT_APPLICATION_MODE))
                .timestamp(getValueOrDefault(headersDTO.getTimestamp(), Instant.now().toString()))
                .customHeaders(headersDTO.getHeaders())
                .build();
    }

    /**
     * Creates a MessageHeadersDTO from a RabbitMQ Message object.
     *
     * @param message the RabbitMQ Message (must not be null)
     * @return a MessageHeadersDTO with values extracted from the message
     */
    public static MessageHeadersDTO createMessageHeadersFromMessage(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        Map<String, Object> messageHeaders = message.getMessageProperties().getHeaders();
        
        return MessageHeadersDTO.builder()
                .origin(extractHeaderValue(messageHeaders, ORIGIN, DEFAULT_ORIGIN))
                .destination(extractHeaderValue(messageHeaders, DESTINATION, DEFAULT_DESTINATION))
                .path(extractHeaderValue(messageHeaders, PATH, DEFAULT_PATH))
                .operation(extractHeaderValue(messageHeaders, OPERATION, DEFAULT_OPERATION))
                .correlationId(extractHeaderValue(messageHeaders, CORRELATIONID, null))
                .userID(extractHeaderValue(messageHeaders, USERID, DEFAULT_USER_ID))
                .applicationMode(extractHeaderValue(messageHeaders, APPLICATION_MODE, DEFAULT_APPLICATION_MODE))
                .timestamp(extractHeaderValue(messageHeaders, TIMESTAMP, null))
                .customHeaders(extractHeadersMap(messageHeaders))
                .build();
    }

    /**
     * Creates a MessageHeadersDTO with all default values.
     *
     * @return a MessageHeadersDTO with default values
     */
    private static MessageHeadersDTO createMessageHeadersWithDefaults() {
        return MessageHeadersDTO.builder()
                .origin(DEFAULT_ORIGIN)
                .destination(DEFAULT_DESTINATION)
                .path(DEFAULT_PATH)
                .operation(DEFAULT_OPERATION)
                .correlationId(generateTraceableCorrelationId())
                .userID(DEFAULT_USER_ID)
                .applicationMode(DEFAULT_APPLICATION_MODE)
                .timestamp(Instant.now().toString())
                .customHeaders(Collections.emptyMap())
                .build();
    }

    private static String getValueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private static String extractHeaderValue(Map<String, Object> headers, String key, String defaultValue) {
        Object value = headers.get(key);
        if (value == null) {
            return defaultValue;
        }
        String stringValue = value.toString();
        return stringValue.isBlank() ? defaultValue : stringValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractHeadersMap(Map<String, Object> messageHeaders) {
        Object value = messageHeaders.get(HEADERS);
        if (value instanceof Map) {
            return (Map<String, String>) value;
        }
        return Collections.emptyMap();
    }

    /**
     * Generates a traceable correlation ID with timestamp and unique identifier.
     * <p>
     * Format: {@code SERVICE_PREFIX-TIMESTAMP-UNIQUE_ID}
     * <p>
     * Example: {@code mop-gateway-20240115-143022-123-abc12345}
     * <p>
     * Structure breakdown:
     * <pre>
     * mop-gateway-20240115-143022-123-abc12345
     * │          │         │      │   │
     * │          │         │      │   └─ Identificador único (8 chars)
     * │          │         │      └─ Milissegundos
     * │          │         └─ Hora:Minuto:Segundo
     * │          └─ Data (YYYYMMDD)
     * └─ Prefixo do serviço
     * </pre>
     * <p>
     * This format provides:
     * <ul>
     *   <li>Service identification for filtering logs by service</li>
     *   <li>Timestamp for chronological ordering and time-based searches</li>
     *   <li>Unique identifier to ensure uniqueness even for concurrent requests</li>
     * </ul>
     *
     * @return a traceable correlation ID string
     */
    public static String generateTraceableCorrelationId() {
        Instant now = Instant.now();
        String timestamp = CORRELATION_ID_TIMESTAMP_FORMATTER.format(now);
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("%s-%s-%s", SERVICE_PREFIX, timestamp, uniqueId);
    }

    /**
     * Extracts the message body as a string from a Message object.
     *
     * @param message the RabbitMQ Message (must not be null)
     * @return the message body as string
     * @throws NullPointerException if message is null
     */
    public static String getBody(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }
}

