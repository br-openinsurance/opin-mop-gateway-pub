package br.com.opin.mopclient.gateway.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for API responses.
 * <p>
 * Provides structured JSON response with detailed information about the request processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "message", "timestamp", "context", "request", "response", "validations",
        "error", "details", "mopReportId"
})
public class ApiResponseDTO {
    
    /**
     * Human-readable message describing the result.
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Error type or code (only present in error responses).
     */
    @JsonProperty("error")
    private String error;
    
    /**
     * Detailed error information (only present in error responses).
     */
    @JsonProperty("details")
    private String details;
    
    /**
     * MOP report ID for request tracking (internal).
     */
    @JsonProperty("mopReportId")
    private String mopReportId;
    
    /**
     * Timestamp when the request was processed (ISO-8601 format).
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * Request tracking context echoed from inbound headers.
     */
    @JsonProperty("context")
    private ResponseContextDTO context;
    
    /**
     * Inbound request metadata echoed from headers.
     */
    @JsonProperty("request")
    private RequestSummaryDTO request;

    /**
     * OpenAPI validation findings grouped by execution status.
     */
    @JsonProperty("validations")
    private ValidationsSummaryDTO validations;

    /**
     * HTTP response from the MOP server when delivery is synchronous (HTTP 200).
     */
    @JsonProperty("response")
    private ServerResponseDTO response;

}
