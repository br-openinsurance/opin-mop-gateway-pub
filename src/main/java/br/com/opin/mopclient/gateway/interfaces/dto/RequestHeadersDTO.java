package br.com.opin.mopclient.gateway.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object (DTO) representing HTTP request headers for anonymization requests.
 * <p>
 * This DTO encapsulates all header information received from HTTP requests and is used
 * to transfer header data from the controller layer to the application and infrastructure layers.
 * It provides a structured way to handle request metadata including routing information,
 * authentication details, and traceability data.
 * <p>
 * <strong>Key Responsibilities:</strong>
 * <ul>
 *   <li>Encapsulates HTTP request headers in a type-safe DTO</li>
 *   <li>Provides builder pattern for easy construction</li>
 *   <li>Supports JSON serialization/deserialization via Jackson annotations</li>
 *   <li>Allows storage of additional custom headers in a map</li>
 * </ul>
 * <p>
     * <strong>Required Headers:</strong>
     * <ul>
     *   <li>{@code origin}: Source system identifier (required)</li>
     *   <li>{@code destination}: Target system identifier (required)</li>
     *   <li>{@code path}: Logical path or endpoint identifier (required)</li>
     *   <li>{@code operation}: Operation type (e.g., CREATE, UPDATE, DELETE, PROCESS) (required)</li>
     *   <li>{@code userID}: User identifier for audit and traceability (required)</li>
     *   <li>{@code applicationMode}: Application mode - TRANSMITTER or RECEIVER (required)</li>
     * </ul>
 * <p>
 * <strong>Optional Headers:</strong>
 * <ul>
 *   <li>{@code correlationID}: Correlation identifier for distributed tracing (optional)</li>
 *   <li>{@code timestamp}: Timestamp of the request (optional, can be auto-generated)</li>
 *   <li>{@code headers}: Map of additional custom headers (optional)</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * // Building from HTTP request headers
 * RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
 *     .origin("external-api-service")
 *     .destination("anonymization-service")
 *     .path("/api/v1/anonymize")
 *     .operation("PROCESS")
     *     .userID("user-12345")
     *     .applicationMode("TRANSMITTER")
     *     .correlationID(UUID.randomUUID().toString())
     *     .timestamp(Instant.now().toString())
     *     .headers(Map.of("x-custom-header", "custom-value"))
     *     .build();
 *
 * // Using in service layer
 * messageService.sendMessageWithHead(jsonPayload, headersDTO);
 * }</pre>
 * <p>
 * <strong>Note:</strong>
 * This DTO is typically created in the controller layer from HTTP request headers
 * and passed to the application layer services. The application layer may convert
 * this DTO to {@link br.com.opin.mopclient.gateway.shared.dto.MessageHeadersDTO}
 * for internal message processing with default values applied where needed.
 *
 * @author MOP Team
 * @since 1.0
 * @see br.com.opin.mopclient.gateway.shared.dto.MessageHeadersDTO
 * @see br.com.opin.mopclient.gateway.interfaces.controller.AnonymizerController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHeadersDTO {

    /**
     * Destination system identifier.
     * <p>
     * Identifies the intended recipient or logical destination for the message
     * in the processing pipeline. This value is used for routing messages to
     * the appropriate queue or service.
     */
    @JsonProperty("destination")
    private String destination;

    /**
     * Origin system identifier.
     * <p>
     * Identifies the system or service that originated the request. This value
     * is used for traceability and routing in the message queue system.
     */
    @JsonProperty("origin")
    private String origin;

    /**
     * Logical path or endpoint identifier.
     * <p>
     * Represents the business route or endpoint related to the message processing.
     * This helps identify the context or domain of the request.
     */
    @JsonProperty("path")
    private String path;

    /**
     * Operation type identifier.
     * <p>
     * Specifies the type of operation being performed (e.g., CREATE, UPDATE, DELETE, PROCESS).
     * Common values include HTTP methods or business operation names.
     */
    @JsonProperty("operation")
    private String operation;

    /**
     * Correlation identifier for distributed tracing.
     * <p>
     * Optional unique identifier used to correlate related messages across
     * different services in a distributed system. If not provided, a new
     * correlation ID may be generated during message processing.
     */
    @JsonProperty("correlationID")
    private String correlationID;

    /**
     * User identifier associated with the request.
     * <p>
     * Identifies the user or system account making the request. Used for
     * audit logging, traceability, and access control purposes.
     */
    @JsonProperty("userID")
    private String userID;

    /**
     * Application mode identifier.
     * <p>
     * Specifies the mode of the application: TRANSMITTER or RECEIVER.
     * This value determines how the message should be processed in the system.
     */
    @JsonProperty("applicationMode")
    private String applicationMode;

    /**
     * Timestamp of the request.
     * <p>
     * Optional timestamp indicating when the request was created or sent.
     * Typically in ISO-8601 format. If not provided, a timestamp may be
     * auto-generated during message processing.
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * Map of additional custom headers.
     * <p>
     * Contains any additional HTTP headers that were present in the original
     * request but are not part of the standard header set. This allows for
     * extensibility and passing through custom metadata.
     */
    private Map<String, String> headers;
}
