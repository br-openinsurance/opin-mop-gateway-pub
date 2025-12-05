package br.com.opin.mopclient.gateway.interfaces.dto.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Main DTO representing the complete process request structure.
 * <p>
 * This DTO encapsulates all information needed for processing a request,
 * including routing information, headers, request details, traceability fields,
 * data payload, and validation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessRequestDTO {

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
     * Logical path or endpoint identifier.
     */
    @JsonProperty("path")
    private String path;

    /**
     * HTTP request headers.
     */
    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * Request information (method, host, path, url).
     */
    @JsonProperty("request")
    private RequestInfoDTO request;

    /**
     * Traceability fields including anonymized/exposed fields and processing context.
     */
    @JsonProperty("traceabilityFields")
    private TraceabilityFieldsDTO traceabilityFields;

    /**
     * Data payload containing business information.
     * Can be empty or contain complex nested structures.
     */
    @JsonProperty("data")
    private Map<String, Object> data;

    /**
     * List of validation errors or warnings.
     */
    @JsonProperty("validations")
    private List<ValidationDTO> validations;
}

