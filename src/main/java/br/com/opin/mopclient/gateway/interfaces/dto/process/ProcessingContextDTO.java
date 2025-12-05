package br.com.opin.mopclient.gateway.interfaces.dto.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing processing context information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessingContextDTO {

    /**
     * Module identifier (e.g., "MOP").
     */
    @JsonProperty("module")
    private String module;

    /**
     * System that initiated the processing.
     */
    @JsonProperty("initiatedBy")
    private String initiatedBy;

    /**
     * Purpose of the data processing.
     */
    @JsonProperty("purpose")
    private String purpose;

    /**
     * Environment identifier (e.g., "dev", "homolog", "prod").
     */
    @JsonProperty("environment")
    private String environment;
}

