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
     * Correlation ID for request tracking.
     */
    @JsonProperty("correlationId")
    private String correlationId;
    
    /**
     * Timestamp when the request was processed (ISO-8601 format).
     */
    @JsonProperty("timestamp")
    private String timestamp;
    
    /**
     * Origin system identifier.
     */
    @JsonProperty("origin")
    private String origin;
    
    /**
     * Destination system identifier.
     */
    @JsonProperty("destination")
    private String destination;
    
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
    
    /**
     * Application mode (TRANSMITTER or RECEIVER).
     */
    @JsonProperty("applicationMode")
    private String applicationMode;
}

