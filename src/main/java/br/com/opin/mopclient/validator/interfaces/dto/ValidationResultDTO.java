package br.com.opin.mopclient.validator.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResultDTO {

    @JsonProperty("validations")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ValidationViolationDTO> validations;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("message")
    private String detailMessage;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("payload")
    private Object validPayload;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("statusCode")
    private HttpStatusCode httpStatusCode;
}
