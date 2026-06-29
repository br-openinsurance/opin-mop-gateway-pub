package br.com.opin.mopclient.gateway.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * HTTP response returned by the MOP server ({@code /process}) on synchronous delivery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerResponseDTO {

    @JsonProperty("status")
    private int status;

    /**
     * Response body from MOP. Parsed as JSON when possible; otherwise the raw text.
     */
    @JsonProperty("body")
    private Object body;

    public static ServerResponseDTO from(ResponseEntity<String> response, ObjectMapper objectMapper) {
        if (response == null) {
            return null;
        }
        String rawBody = response.getBody();
        Object parsedBody = null;
        if (StringUtils.hasText(rawBody)) {
            try {
                parsedBody = objectMapper.readValue(rawBody, Object.class);
            } catch (Exception e) {
                parsedBody = rawBody;
            }
        }
        return ServerResponseDTO.builder()
                .status(response.getStatusCode().value())
                .body(parsedBody)
                .build();
    }
}
