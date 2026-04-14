package br.com.opin.mopclient.gateway.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ApiResponseDTO {
    
    /**
     * Response status (e.g., "SUCCESS", "ERROR").
     */
    @JsonProperty("status")
    private String status;
    
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
     * Correlation ID for request tracking (informed by user in header X-Correlation-Id).
     * Response does not include trace object; trace exists only in the final JSON (MessageDTO).
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * MOP report ID for request tracking (internal).
     */
    @JsonProperty("mopReportid")
    private String mopReportid;
    
    /**
     * Timestamp when the request was processed (ISO-8601 format).
     */
    @JsonProperty("timestamp")
    private String timestamp;
    
    /**
     * Client SS (insurer / source system) identifier.
     */
    @JsonProperty("clientSSId")
    private String clientSSId;
    
    /**
     * Server AS (insurer / destination system) identifier.
     */
    @JsonProperty("serverASId")
    private String serverASId;
    
    /**
     * Path of the request.
     */
    @JsonProperty("path")
    private String path;
    
    /**
     * Operation type (e.g., POST, GET).
     */
    @JsonProperty("operation")
    private String operation;
    
}
