package br.com.opin.mopclient.retry.application;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.retry.domain.ClientRetryFailureStage;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ClientRetryEnqueueService {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryEnqueueService.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public ClientRetryEnqueueService(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${mop.client.retry.queue}") String queueName) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.queueName = Objects.requireNonNull(queueName, "mop.client.retry.queue must be configured");
    }

    public void enqueue(
            ClientRetryFailureStage stage,
            String detail,
            String originalRequestBody,
            String normalizedRequestPayload,
            RequestHeadersDTO headers,
            String wrappedPayloadForProcess) {

        String correlationId = headers != null && headers.getCorrelationId() != null
                ? headers.getCorrelationId()
                : "unknown";

        ClientRetryMessage message = ClientRetryMessage.builder()
                .failureStage(stage)
                .detail(truncate(detail, 4000))
                .correlationId(correlationId)
                .enqueuedAt(Instant.now().toString())
                .originalRequestBody(originalRequestBody)
                .requestPayload(normalizedRequestPayload)
                .headers(snapshotHeaders(headers))
                .wrappedPayloadForProcess(wrappedPayloadForProcess)
                .build();

        try {
            byte[] body = objectMapper.writeValueAsBytes(message);
            rabbitTemplate.convertAndSend("", queueName, body, m -> {
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                m.getMessageProperties().setContentType("application/json");
                return m;
            });
            logger.warn(
                    "[MOP retry] Message written to queue (MOP service unavailable) | queue={} | stage={} | correlationId={} | summary={}",
                    queueName,
                    friendlyStageLabel(stage),
                    correlationId,
                    oneLineSummary(detail));
            if (logger.isDebugEnabled()) {
                logger.debug("[MOP retry] Full technical detail | correlationId={} | detail={}", correlationId, detail);
            }
        } catch (Exception e) {
            logger.error(
                    "[MOP retry] Failed to publish to queue {} | correlationId={} | {}",
                    queueName,
                    correlationId,
                    e.getMessage(),
                    e);
            throw new IllegalStateException("Failed to publish message to client retry queue", e);
        }
    }

    /**
     * Immutable snapshot so later mutations on the live {@link RequestHeadersDTO} do not alter the queued message.
     */
    private static RequestHeadersDTO snapshotHeaders(RequestHeadersDTO h) {
        if (h == null) {
            return null;
        }
        Map<String, String> raw = h.getHeaders();
        Map<String, String> headersCopy = raw == null ? null : new LinkedHashMap<>(raw);
        return RequestHeadersDTO.builder()
                .clientSSId(h.getClientSSId())
                .serverASId(h.getServerASId())
                .origin(h.getOrigin())
                .path(h.getPath())
                .operation(h.getOperation())
                .correlationId(h.getCorrelationId())
                .mopReportid(h.getMopReportid())
                .timestamp(h.getTimestamp())
                .headers(headersCopy)
                .step(h.getStep())
                .dataEventoStep(h.getDataEventoStep())
                .traceOrigin(h.getTraceOrigin())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String friendlyStageLabel(ClientRetryFailureStage stage) {
        if (stage == null) {
            return "unknown";
        }
        return switch (stage) {
            case ANONYMIZATION_CONFIG -> "config (GET anonymization-fields)";
            case PROCESS_ENDPOINT -> "submit (POST /process)";
        };
    }

    /** Short single-line text for WARN; full detail is logged at DEBUG. */
    private static String oneLineSummary(String detail) {
        if (detail == null || detail.isBlank()) {
            return "(no detail)";
        }
        String one = detail.replace('\r', ' ').replace('\n', ' ').replace('|', ' ').trim();
        while (one.contains("  ")) {
            one = one.replace("  ", " ");
        }
        return truncate(one, 160);
    }
}
