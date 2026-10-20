package br.com.opin.mopclient.consentfunnel.interfaces.dto;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.shared.ConsentFunnelDateTime;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body alinhado a {@code swagger/consent-funnel/consent-funnel.yml}.
 * Somente: consentId, step, origin, correlationId, additionalInfo, timestamp,
 * clientSSId, serverASId e permissions. Qualquer outro campo na raiz é rejeitado.
 * additionalInfo é opcional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Evento do funil de consentimentos (ConsentCreated)")
public class ConsentFunnelEventRequestDTO {

    public static final String UUID_PATTERN = "^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$";
    public static final String ORIGIN_PATTERN = "^(?i)(CLIENT|SERVER)$";

    @NotBlank(message = "The field consentId is required and cannot be empty")
    @Schema(example = "urn:bancoex:C1DD33123")
    private String consentId;

    @NotBlank(message = "The field step is required and cannot be empty")
    @Schema(example = "consent-created")
    private String step;

    @NotBlank(message = "The field origin is required and cannot be empty")
    @Pattern(regexp = ORIGIN_PATTERN, message = "origin must be CLIENT or SERVER")
    @Schema(example = "CLIENT", allowableValues = {"CLIENT", "SERVER"})
    private String origin;

    @NotBlank(message = "The field correlationId is required and cannot be empty")
    @Pattern(regexp = UUID_PATTERN, message = "correlationId must be a UUID")
    @Schema(example = "577869e5-4c63-4b19-9235-a18d22c80986")
    private String correlationId;

    @Valid
    @Schema(description = "Opcional. Só as chaves conhecidas; todas opcionais. Reenviado ao PCM quando presente.")
    private ConsentFunnelAdditionalInfoDTO additionalInfo;

    @NotBlank(message = "The field timestamp is required and cannot be empty")
    @Pattern(
            regexp = ConsentFunnelDateTime.REGEX,
            message = "The field timestamp must match yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2026-10-14T19:34:38")
    private String timestamp;

    @NotBlank(message = "The field clientSSId is required and cannot be empty")
    @Pattern(regexp = UUID_PATTERN, message = "clientSSId must be a UUID")
    @Schema(example = "2a59c2a3-529f-41c6-97e3-77395e9951ca")
    private String clientSSId;

    @NotBlank(message = "The field serverASId is required and cannot be empty")
    @Pattern(regexp = UUID_PATTERN, message = "serverASId must be a UUID")
    @Schema(example = "f8cd7b48-197d-419b-8680-f42226111b6f")
    private String serverASId;

    @NotEmpty(message = "The field permissions is required and cannot be empty")
    @Size(min = 1, message = "The field permissions is required and cannot be empty")
    @Schema(description = "Permissões do consentimento")
    private List<@NotBlank(message = "The field permissions is required and cannot contain empty items") String> permissions;

    @JsonAnySetter
    void rejectUnknownProperty(String name, Object ignored) {
        throw ConsentFunnelValidationException.single(
                name,
                "Unknown property '" + name + "' is not allowed",
                "ADDITIONAL_PROPERTIES");
    }
}
