package br.com.opin.mopclient.gateway.interfaces.dto.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing traceability fields for data processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceabilityFieldsDTO {

    /**
     * List of field names that should be anonymized.
     */
    @JsonProperty("anonymized")
    private List<String> anonymized;

    /**
     * List of field names that should be exposed (not anonymized).
     */
    @JsonProperty("exposed")
    private List<String> exposed;

    /**
     * Processing context information.
     */
    @JsonProperty("processingContext")
    private ProcessingContextDTO processingContext;
}

