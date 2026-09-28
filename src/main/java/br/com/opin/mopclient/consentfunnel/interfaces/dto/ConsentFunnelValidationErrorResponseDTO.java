package br.com.opin.mopclient.consentfunnel.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resposta de erro de validação do endpoint de funil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Erro de validação do funil de consentimentos")
public class ConsentFunnelValidationErrorResponseDTO {

    @Schema(example = "ERROR")
    private String status;

    @Schema(example = "3")
    private int total;

    @JsonProperty("pending")
    private List<ConsentFunnelViolationDTO> pending;
}
