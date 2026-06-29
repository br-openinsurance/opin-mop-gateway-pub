package br.com.opin.mopclient.retry.infrastructure.inbound;

import br.com.opin.mopclient.retry.application.ClientRetryFailureHandler;
import br.com.opin.mopclient.retry.application.ClientRetryReplayService;
import br.com.opin.mopclient.retry.application.MopServerAvailabilityProbe;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the retry queue while MOP is reachable and the {@code mopProcessEndpoint} circuit breaker is not open.
 *
 * <p>Ack semantics: success → ack; unparseable message → DLQ via {@link ClientRetryFailureHandler} then ack;
 * MOP/business failure → re-publish to retry (incrementing attempts) or DLQ when max attempts reached, then ack;
 * the current tick stops early after a replay failure to avoid cascading failures.
 * Availability and breaker state are rechecked before every message, not only at tick start.
 * <p>Com {@code spring.main.lazy-initialization=true} na app, este bean tem de ser eager:
 * não é injetado por mais ninguém; só métodos agendados — caso contrário o dreno nunca arranca.
 */
@Component
@Lazy(false)
@ConditionalOnProperty(name = "mop.client.retry.replay.enabled", havingValue = "true", matchIfMissing = true)
public class ClientRetryReplayScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryReplayScheduler.class);

    private static final String PROCESS_ENDPOINT_CIRCUIT_BREAKER = "mopProcessEndpoint";

    /** Log copy when drain is skipped because the periodic GET anonymization-config probe reports MOP down. */
    private static final String DRAIN_SKIP_UNAVAILABLE_LOG =
            "Operation could not be executed because the service is currently unavailable. "
                    + "The last check failed. The process will be retried | %s";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ClientRetryReplayService replayService;
    private final ClientRetryFailureHandler failureHandler;
    private final MopServerAvailabilityProbe availabilityProbe;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final String queueName;
    private final int maxMessagesPerTick;
    private final String processEndpointUrl;

    public ClientRetryReplayScheduler(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            ClientRetryReplayService replayService,
            ClientRetryFailureHandler failureHandler,
            MopServerAvailabilityProbe availabilityProbe,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${mop.client.retry.queue}") String queueName,
            @Value("${mop.client.retry.replay.max-messages-per-tick:25}") int maxMessagesPerTick,
            @Value("${mop.endpoints.process.url}") String processEndpointUrl) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.replayService = replayService;
        this.failureHandler = failureHandler;
        this.availabilityProbe = availabilityProbe;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.queueName = queueName;
        this.maxMessagesPerTick = Math.max(1, maxMessagesPerTick);
        this.processEndpointUrl = processEndpointUrl;
    }

    @Scheduled(
            fixedDelayString = "${mop.client.retry.replay.interval-ms:60000}",
            initialDelayString = "${mop.client.retry.replay.initial-delay-ms:60000}")
    public void drainRetryQueueWhenMopAvailable() {
        if (!isEndpointReady()) {
            logger.info("[MOP retry] Operation skipped | {}", endpointNotReadyReason());
            return;
        }
        for (int i = 0; i < maxMessagesPerTick; i++) {
            if (!isEndpointReady()) {
                logger.info(
                        "[MOP retry] Stopping drain early | {} | processed={}/{}",
                        endpointNotReadyReason(),
                        i,
                        maxMessagesPerTick);
                break;
            }
            Boolean consumed = rabbitTemplate.execute(this::consumeOne);
            if (Boolean.FALSE.equals(consumed)) {
                break;
            }
        }
    }

    private boolean isEndpointReady() {
        if (!availabilityProbe.isServerAvailable()) {
            return false;
        }
        CircuitBreaker.State state = circuitBreakerRegistry
                .circuitBreaker(PROCESS_ENDPOINT_CIRCUIT_BREAKER)
                .getState();
        return state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN;
    }

    private String endpointNotReadyReason() {
        if (!availabilityProbe.isServerAvailable()) {
            return String.format(
                    DRAIN_SKIP_UNAVAILABLE_LOG,
                    availabilityProbe.getAnonymizationConfigProbeUrl());
        }
        CircuitBreaker.State state = circuitBreakerRegistry
                .circuitBreaker(PROCESS_ENDPOINT_CIRCUIT_BREAKER)
                .getState();
        return String.format(
                "circuit %s = %s (need CLOSED or HALF_OPEN) | POST %s",
                PROCESS_ENDPOINT_CIRCUIT_BREAKER,
                state,
                processEndpointUrl);
    }

    private Boolean consumeOne(Channel channel) {
        try {
            GetResponse response = channel.basicGet(queueName, false);
            if (response == null) {
                return false;
            }
            long tag = response.getEnvelope().getDeliveryTag();
            byte[] body = response.getBody();
            try {
                ClientRetryMessage message = objectMapper.readValue(body, ClientRetryMessage.class);
                replayService.replay(message);
                channel.basicAck(tag, false);
                return true;
            } catch (IllegalArgumentException | JsonProcessingException parseEx) {
                logger.error(
                        "Invalid retry message, moving to DLQ | deliveryTag={} | {}",
                        tag,
                        parseEx.getMessage());
                failureHandler.moveUnparseableToDlq(body, parseEx.getMessage());
                channel.basicAck(tag, false);
                return true;
            } catch (Exception ex) {
                logger.warn(
                        "Replay failed | deliveryTag={} | {}",
                        tag, ex.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Replay failure detail | deliveryTag={}", tag, ex);
                }
                try {
                    ClientRetryMessage message = objectMapper.readValue(body, ClientRetryMessage.class);
                    failureHandler.handleReplayFailure(message, ex.getMessage());
                    channel.basicAck(tag, false);
                } catch (JsonProcessingException parseOnFailure) {
                    failureHandler.moveUnparseableToDlq(body, parseOnFailure.getMessage());
                    channel.basicAck(tag, false);
                }
                return false;
            }
        } catch (Exception e) {
            logger.error("RabbitMQ error while draining retry queue | {}", e.getMessage(), e);
            return false;
        }
    }
}
