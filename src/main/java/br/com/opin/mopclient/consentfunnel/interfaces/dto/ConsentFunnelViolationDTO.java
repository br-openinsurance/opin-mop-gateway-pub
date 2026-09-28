package br.com.opin.mopclient.consentfunnel.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Violação de validação exposta na resposta HTTP do funil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalhe de violação de validação do funil")
public class ConsentFunnelViolationDTO {

    @JsonProperty("violation")
    private String violation;

    @JsonProperty("code")
    private String code;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("attribute")
    private String attribute;
}
