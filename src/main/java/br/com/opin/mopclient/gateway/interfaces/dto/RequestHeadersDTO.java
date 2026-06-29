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
 * <strong>Required HTTP headers (controller):</strong>
 * <ul>
 *   <li>{@code X-Correlation-Id}, {@code origin}, {@code path}, {@code operation}, {@code httpType}</li>
 *   <li>{@code statusCode}: obrigatório quando {@code httpType} é {@code Response}; opcional quando {@code Request}</li>
 * </ul>
 * <p>
 * <strong>Optional Headers:</strong>
 * <ul>
 *   <li>{@code clientSSId}, {@code serverASId}: identificadores das partes quando informados</li>
 *   <li>{@code traceOrigin}: origem do evento de trace quando informada</li>
 *   <li>{@code mopReportid}: MOP report identifier for distributed tracing (optional)</li>
 *   <li>{@code timestamp}: Timestamp of the request (optional, can be auto-generated)</li>
 *   <li>{@code headers}: Map of additional custom headers (optional)</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * // Building from HTTP request headers
 * RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
 *     .origin("client")
 *     .path("/open-insurance/consents/v2/consents")
 *     .operation("POST")
 *     .mopReportid(UUID.randomUUID().toString())
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
 * and passed to the application layer services for unified flow processing.
 *
 * @author MOP Team
 * @since 1.0
 * @see br.com.opin.mopclient.gateway.interfaces.controller.AnonymizerController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHeadersDTO {

    /**
     * Client SS (insurer / source system) identifier.
     * <p>
     * Identifies the client/receiver participant (e.g. receiver party A).
     */
    @JsonProperty("clientSSId")
    private String clientSSId;

    /**
     * Server AS (insurer / destination system) identifier.
     * <p>
     * Identifies the server/transmitter participant (e.g. transmitter party B).
     */
    @JsonProperty("serverASId")
    private String serverASId;

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
     * HTTP message type: {@code Request} or {@code Response} (required).
     */
    @JsonProperty("httpType")
    private String httpType;

    /**
     * HTTP status code. Required when {@code httpType} is {@code Response}; optional when {@code Request}.
     */
    @JsonProperty("statusCode")
    private String statusCode;

    /**
     * Correlation ID (required). Provided by the client in the X-Correlation-Id header.
     * Independent field used for tracing.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * MOP report identifier for distributed tracing (internal/legacy).
     */
    @JsonProperty("mopReportid")
    private String mopReportid;

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

    /**
     * Origin of the event in the trace (e.g. CLIENT, SERVER).
     * <p>
     * Optional. When provided via header {@code traceOrigin}, it is serialized in {@code TraceDTO} of the {@code MessageDTO} sent to the MOP server.
     */
    @JsonProperty("traceOrigin")
    private String traceOrigin;
}
