package br.com.opin.mopclient.retry.infrastructure.inbound;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the retry queue while MOP is reachable and the {@code mopProcessEndpoint} circuit breaker is not open.
 *
 * <p>Ack semantics: success → ack; unparseable message → nack without requeue (poison-message guard);
 * MOP/business failure → nack with requeue and the current tick stops early to avoid cascading failures.
 * Availability and breaker state are rechecked before every message, not only at tick start.
 */
@Component
@ConditionalOnProperty(name = "mop.client.retry.replay.enabled", havingValue = "true", matchIfMissing = true)
public class ClientRetryReplayScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryReplayScheduler.class);

    private static final String PROCESS_ENDPOINT_CIRCUIT_BREAKER = "mopProcessEndpoint";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ClientRetryReplayService replayService;
    private final MopServerAvailabilityProbe availabilityProbe;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final String queueName;
    private final int maxMessagesPerTick;

    public ClientRetryReplayScheduler(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            ClientRetryReplayService replayService,
            MopServerAvailabilityProbe availabilityProbe,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${mop.client.retry.queue}") String queueName,
            @Value("${mop.client.retry.replay.max-messages-per-tick:25}") int maxMessagesPerTick) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.replayService = replayService;
        this.availabilityProbe = availabilityProbe;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.queueName = queueName;
        this.maxMessagesPerTick = Math.max(1, maxMessagesPerTick);
    }

    @Scheduled(
            fixedDelayString = "${mop.client.retry.replay.interval-ms:10000}",
            initialDelayString = "${mop.client.retry.replay.initial-delay-ms:15000}")
    public void drainRetryQueueWhenMopAvailable() {
        if (!isEndpointReady()) {
            return;
        }
        for (int i = 0; i < maxMessagesPerTick; i++) {
            if (!isEndpointReady()) {
                logger.info(
                        "Stopping retry drain early | reason=endpoint not ready (availability or circuit breaker state) | processed={}/{}",
                        i, maxMessagesPerTick);
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
                        "Discarding invalid retry message (parse/validation) | deliveryTag={} | {}",
                        tag,
                        parseEx.getMessage());
                channel.basicNack(tag, false, false);
                return true;
            } catch (Exception ex) {
                logger.warn(
                        "Replay failed, message kept on the retry queue | deliveryTag={} | {}",
                        tag, ex.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Replay failure detail | deliveryTag={}", tag, ex);
                }
                channel.basicNack(tag, false, true);
                return false;
            }
        } catch (Exception e) {
            logger.error("RabbitMQ error while draining retry queue | {}", e.getMessage(), e);
            return false;
        }
    }
}
