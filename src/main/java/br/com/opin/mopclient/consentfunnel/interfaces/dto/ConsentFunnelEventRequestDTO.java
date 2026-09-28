package br.com.opin.mopclient.consentfunnel.interfaces.dto;

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
 * Request body alinhado a {@code swagger/consent-funnel/consent-funnel.yml} (schema {@code ConsentCreated}).
 * Todos os campos de raiz são obrigatórios; apenas as chaves de {@code additionalInfo} são opcionais.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Evento do funil de consentimentos (ConsentCreated)")
public class ConsentFunnelEventRequestDTO {

    public static final String UUID_PATTERN = "^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$";
    public static final String ORIGIN_PATTERN = "^(?i)(CLIENT|SERVER)$";

    @NotBlank
    @Schema(example = "urn:bancoex:C1DD33123")
    private String consentId;

    @NotBlank
    @Schema(example = "consent-created")
    private String step;

    @NotBlank
    @Pattern(regexp = ORIGIN_PATTERN, message = "origin must be CLIENT or SERVER")
    @Schema(example = "CLIENT", allowableValues = {"CLIENT", "SERVER"})
    private String origin;

    @NotBlank
    @Schema(example = "577869e5-4c63-4b19-9235-a18d22c80986")
    private String correlationId;

    @Valid
    @Schema(description = "Informações complementares opcionais")
    private ConsentFunnelAdditionalInfoDTO additionalInfo;

    @NotBlank
    @Schema(example = "2022-11-07T17:26:32Z")
    private String timestamp;

    @NotBlank
    @Pattern(regexp = UUID_PATTERN, message = "clientOrgId must be a UUID")
    @Schema(example = "1fb79963-4bff-4204-9370-93aceb8a2f0d")
    private String clientOrgId;

    @NotBlank
    @Pattern(regexp = UUID_PATTERN, message = "clientSSId must be a UUID")
    @Schema(example = "2a59c2a3-529f-41c6-97e3-77395e9951ca")
    private String clientSSId;

    @NotBlank
    @Pattern(regexp = UUID_PATTERN, message = "serverOrgId must be a UUID")
    @Schema(example = "ff66b95a-d817-4fbe-949a-c5912e240189")
    private String serverOrgId;

    @NotBlank
    @Pattern(regexp = UUID_PATTERN, message = "serverASId must be a UUID")
    @Schema(example = "f8cd7b48-197d-419b-8680-f42226111b6f")
    private String serverASId;

    @NotEmpty
    @Size(min = 1, message = "permissions must not be empty")
    @Schema(description = "Permissões do consentimento")
    private List<@NotBlank String> permissions;
}
