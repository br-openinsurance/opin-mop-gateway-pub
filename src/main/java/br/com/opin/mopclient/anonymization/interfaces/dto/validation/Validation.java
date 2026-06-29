package br.com.opin.mopclient.anonymization.interfaces.dto.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Validation {

    @JsonProperty("violation")
    private String violation;

    @JsonProperty("code")
    private String code;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("attribute")
    private String attribute;
}
