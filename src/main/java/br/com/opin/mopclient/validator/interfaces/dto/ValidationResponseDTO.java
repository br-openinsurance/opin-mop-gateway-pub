package br.com.opin.mopclient.validator.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the complete response of an OpenAPI validation.
 */
@Data
@NoArgsConstructor
public class ValidationResponseDTO {

    @JsonProperty("data")
    private ValidationResultDTO validationResult = new ValidationResultDTO();
}
