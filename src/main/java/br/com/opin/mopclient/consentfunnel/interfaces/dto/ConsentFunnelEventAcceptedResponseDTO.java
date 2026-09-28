package br.com.opin.mopclient.consentfunnel.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta de aceite do evento de funil recebido pelo MOP Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confirmação de recebimento do evento de funil")
public class ConsentFunnelEventAcceptedResponseDTO {

    @Schema(example = "ACCEPTED")
    private String status;

    @Schema(example = "577869e5-4c63-4b19-9235-a18d22c80986")
    private String correlationId;

    @Schema(example = "urn:bancoex:C1DD33123")
    private String consentId;

    @Schema(example = "consent-created")
    private String step;

    @Schema(example = "2026-07-29T12:00:00Z")
    private String receivedAt;
}
