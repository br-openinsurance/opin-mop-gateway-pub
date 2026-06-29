package br.com.opin.mopclient.validator.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ValidationViolationDTO {
    
    @JsonProperty("violation")
    private String message;

    @JsonProperty("code")
    private String code;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("attribute")
    private String crumbs;

    public ValidationViolationDTO(String message, String code, String severity, String crumbs) {
        this.message = message;
        this.code = code;
        this.severity = severity;
        this.crumbs = crumbs;
    }
}
