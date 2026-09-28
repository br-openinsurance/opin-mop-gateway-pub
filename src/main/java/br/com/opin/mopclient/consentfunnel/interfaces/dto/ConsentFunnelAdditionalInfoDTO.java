package br.com.opin.mopclient.consentfunnel.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Campos de {@code additionalInfo} conforme {@code swagger/consent-funnel/consent-funnel.yml}.
 * Todos opcionais; valores livres (string).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Informações complementares do evento do funil")
public class ConsentFunnelAdditionalInfoDTO {

    @JsonProperty("consent-user")
    @Schema(example = "invalid-credentials")
    private String consentUser;

    @JsonProperty("authentication-failure-reason")
    @Schema(example = "invalid-credentials")
    private String authenticationFailureReason;

    @JsonProperty("user-redirected-back-status")
    @Schema(example = "success")
    private String userRedirectedBackStatus;

    @JsonProperty("token-kind")
    @Schema(example = "consent-token")
    private String tokenKind;

    @JsonProperty("rejected-by")
    @Schema(example = "user")
    private String rejectedBy;

    @JsonProperty("revoked-by")
    @Schema(example = "user")
    private String revokedBy;

    @JsonProperty("expired-by")
    @Schema(example = "authorization-timeout")
    private String expiredBy;
}
