package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import br.com.opin.mopclient.gateway.interfaces.dto.ValidationsSummaryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"method", "host", "url", "header", "validations"})
public class HttpRequestInfoDTO {

    @JsonProperty("method")
    private String method;

    @JsonProperty("host")
    private String host;

    @JsonProperty("url")
    private String url;

    @JsonProperty("header")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> header;

    @JsonProperty("validations")
    private ValidationsSummaryDTO validations;
}
