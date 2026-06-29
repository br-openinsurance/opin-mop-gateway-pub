package br.com.opin.mopclient.gateway.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request tracking context echoed in gateway responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseContextDTO {

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("clientSSId")
    private String clientSSId;

    @JsonProperty("serverASId")
    private String serverASId;
}
