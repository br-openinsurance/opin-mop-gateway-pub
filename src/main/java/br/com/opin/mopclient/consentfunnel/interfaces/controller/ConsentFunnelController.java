package br.com.opin.mopclient.consentfunnel.interfaces.controller;

import br.com.opin.mopclient.consentfunnel.application.port.in.SubmitConsentFunnelEventUseCase;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventAcceptedResponseDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Ingresso de eventos do funil de consentimentos (PCM) no MOP Client.
 * <p>
 * <strong>Independente</strong> do fluxo {@code POST /data}: não usa fila de retry do gateway,
 * anonimização, {@code MessageDTO} nem envio ao MOP {@code /process}. Apenas valida e aceita o evento PCM.
 * <p>
 * URL completa com {@code context-path=/v1}: {@code POST /v1/data-funil-consents}.
 * Sem o prefixo {@code /anonymize} — esse vale só para o rastreio ({@code POST /v1/anonymize/data}).
 */
@RestController
@RequestMapping("/data-funil-consents")
@Tag(name = "Consent Funnel", description = "Ingresso de eventos do funil de consentimentos (PCM)")
public class ConsentFunnelController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentFunnelController.class);

    private final SubmitConsentFunnelEventUseCase submitConsentFunnelEventUseCase;

    public ConsentFunnelController(SubmitConsentFunnelEventUseCase submitConsentFunnelEventUseCase) {
        this.submitConsentFunnelEventUseCase = Objects.requireNonNull(submitConsentFunnelEventUseCase);
    }

    @Operation(
            summary = "Recebe evento unitário do funil de consentimentos",
            description = """
                    Aceita payload JSON alinhado a `swagger/consent-funnel/consent-funnel.yml` (schema `ConsentCreated`).
                    Validação: Bean Validation nos quatro IDs obrigatórios e OpenAPI (openapi4j).
                    **Não** enfileira nem envia ao MOP Server — fluxo independente de `POST /data`.
                    Evento aceito fica disponível para processamento downstream do funil.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Evento aceito para processamento",
                    content = @Content(schema = @Schema(implementation = ConsentFunnelEventAcceptedResponseDTO.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload ou regras de funil inválidas",
                    content = @Content(schema = @Schema(implementation = br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelValidationErrorResponseDTO.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConsentFunnelEventAcceptedResponseDTO> submitEvent(
            @Valid @RequestBody ConsentFunnelEventRequestDTO request) {

        LOGGER.debug(
                "Received consent funnel event | correlationId={} | consentId={} | step={}",
                request.getCorrelationId(),
                request.getConsentId(),
                request.getStep());

        ConsentFunnelEventAcceptedResponseDTO response = submitConsentFunnelEventUseCase.submit(request);
        return ResponseEntity.accepted().body(response);
    }
}
