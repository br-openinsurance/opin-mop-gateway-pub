package br.com.opin.mopclient.retry.application;

import br.com.opin.mopclient.gateway.application.service.ProcessingOrchestratorService;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Re-runs the gateway processing flow for a message taken from {@code mop.client.retry.queue}.
 * Does not enqueue again on MOP failure (the message stays the consumer's responsibility to requeue).
 */
@Service
public class ClientRetryReplayService {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryReplayService.class);

    private final ProcessingOrchestratorService orchestratorService;

    public ClientRetryReplayService(ProcessingOrchestratorService orchestratorService) {
        this.orchestratorService = Objects.requireNonNull(orchestratorService, "ProcessingOrchestratorService cannot be null");
    }

    public void replay(ClientRetryMessage message) {
        Objects.requireNonNull(message, "message cannot be null");
        if (message.getHeaders() == null) {
            throw new IllegalArgumentException("Retry message has no headers snapshot");
        }

        String normalized = message.getRequestPayload();
        String original = message.getOriginalRequestBody();
        if (!StringUtils.hasText(original)) {
            original = normalized;
        }
        if (!StringUtils.hasText(normalized)) {
            normalized = original;
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Retry message has no request body or normalized payload");
        }

        String correlationId = message.getCorrelationId() != null ? message.getCorrelationId() : "unknown";
        logger.info("Replaying client retry message | correlationId={} | stage={}", correlationId, message.getFailureStage());

        orchestratorService.processRequest(original, normalized, message.getHeaders(), true);
        logger.info("Replay finished successfully | correlationId={}", correlationId);
    }
}
