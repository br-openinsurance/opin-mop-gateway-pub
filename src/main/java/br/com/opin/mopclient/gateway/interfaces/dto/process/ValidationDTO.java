package br.com.opin.mopclient.gateway.interfaces.dto.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a validation error or warning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationDTO {

    /**
     * Violation message describing the validation error.
     */
    @JsonProperty("violation")
    private String violation;

    /**
     * Error code associated with the validation.
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * Severity level (e.g., "ERROR", "WARNING", "INFO").
     */
    @JsonProperty("severity")
    private String severity;

    /**
     * Attribute or field path where the violation occurred.
     */
    @JsonProperty("attribute")
    private String attribute;
}

