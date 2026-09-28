package br.com.opin.mopclient.consentfunnel.application.service;

import br.com.opin.mopclient.consentfunnel.application.mapper.ConsentFunnelEventMapper;
import br.com.opin.mopclient.consentfunnel.application.port.in.SubmitConsentFunnelEventUseCase;
import br.com.opin.mopclient.consentfunnel.application.port.out.ConsentFunnelMetricsPort;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelEvent;
import br.com.opin.mopclient.consentfunnel.infrastructure.validation.ConsentFunnelOpenApiValidator;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventAcceptedResponseDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Orquestra validação (Bean Validation no controller e OpenAPI {@code consent-funnel.yml}),
 * envia o evento assinado (JWT) ao endpoint de métricas PCM e retorna aceite ao caller.
 * <p>
 * Sem integração com {@code ProcessingOrchestratorService}, fila de retry ou API MOP {@code /process}.
 */
@Service
public class ConsentFunnelEventService implements SubmitConsentFunnelEventUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentFunnelEventService.class);
    public static final String ACCEPTED_STATUS = "ACCEPTED";

    private final ConsentFunnelOpenApiValidator openApiValidator;
    private final ConsentFunnelMetricsPort metricsPort;

    public ConsentFunnelEventService(
            ConsentFunnelOpenApiValidator openApiValidator,
            ConsentFunnelMetricsPort metricsPort) {
        this.openApiValidator = Objects.requireNonNull(openApiValidator);
        this.metricsPort = Objects.requireNonNull(metricsPort);
    }

    @Override
    public ConsentFunnelEventAcceptedResponseDTO submit(ConsentFunnelEventRequestDTO request) {
        Objects.requireNonNull(request, "request");

        openApiValidator.validate(request);

        ConsentFunnelEvent event = ConsentFunnelEventMapper.toDomain(request);

        metricsPort.submit(event.correlationId(), request);

        LOGGER.info(
                "Consent funnel event accepted | correlationId={} | consentId={} | step={}",
                event.correlationId(),
                event.consentId(),
                event.step() != null ? event.step().getValue() : request.getStep());

        return ConsentFunnelEventAcceptedResponseDTO.builder()
                .status(ACCEPTED_STATUS)
                .correlationId(event.correlationId())
                .consentId(event.consentId())
                .step(event.step() != null ? event.step().getValue() : request.getStep())
                .receivedAt(Instant.now().toString())
                .build();
    }
}
