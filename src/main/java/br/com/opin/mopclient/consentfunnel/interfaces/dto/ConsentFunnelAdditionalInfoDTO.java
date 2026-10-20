package br.com.opin.mopclient.consentfunnel.interfaces.dto;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * additionalInfo: objeto opcional. Só as sete chaves abaixo, todas opcionais.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
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

    @JsonAnySetter
    void rejectUnknownProperty(String name, Object ignored) {
        throw ConsentFunnelValidationException.single(
                "additionalInfo." + name,
                "Unknown property 'additionalInfo." + name + "' is not allowed",
                "ADDITIONAL_PROPERTIES");
    }
}
