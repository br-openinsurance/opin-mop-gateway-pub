package br.com.opin.mopclient.gateway.interfaces.dto.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing HTTP request information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestInfoDTO {

    /**
     * HTTP method (e.g., GET, POST, PUT, DELETE).
     */
    @JsonProperty("method")
    private String method;

    /**
     * Host name or IP address.
     */
    @JsonProperty("host")
    private String host;

    /**
     * Request path.
     */
    @JsonProperty("path")
    private String path;

    /**
     * Complete request URL.
     */
    @JsonProperty("url")
    private String url;
}

