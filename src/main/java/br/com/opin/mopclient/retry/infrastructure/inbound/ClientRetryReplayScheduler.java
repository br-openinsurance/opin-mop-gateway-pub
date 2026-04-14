package br.com.opin.mopclient.retry.infrastructure.inbound;

import br.com.opin.mopclient.retry.application.ClientRetryReplayService;
import br.com.opin.mopclient.retry.application.MopServerAvailabilityProbe;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * When MOP is reported available, pulls messages from the retry queue and reprocesses them with manual ack/nack.
 */
@Component
@ConditionalOnProperty(name = "mop.client.retry.replay.enabled", havingValue = "true", matchIfMissing = true)
public class ClientRetryReplayScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryReplayScheduler.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ClientRetryReplayService replayService;
    private final MopServerAvailabilityProbe availabilityProbe;
    private final String queueName;
    private final int maxMessagesPerTick;

    public ClientRetryReplayScheduler(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            ClientRetryReplayService replayService,
            MopServerAvailabilityProbe availabilityProbe,
            @Value("${mop.client.retry.queue}") String queueName,
            @Value("${mop.client.retry.replay.max-messages-per-tick:25}") int maxMessagesPerTick) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.replayService = replayService;
        this.availabilityProbe = availabilityProbe;
        this.queueName = queueName;
        this.maxMessagesPerTick = Math.max(1, maxMessagesPerTick);
    }

    @Scheduled(fixedDelayString = "${mop.client.retry.replay.interval-ms:10000}", initialDelayString = "15000")
    public void drainRetryQueueWhenMopAvailable() {
        if (!availabilityProbe.isServerAvailable()) {
            return;
        }
        for (int i = 0; i < maxMessagesPerTick; i++) {
            Boolean consumed = rabbitTemplate.execute(this::consumeOne);
            if (Boolean.FALSE.equals(consumed)) {
                break;
            }
        }
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
                logger.warn("Replay failed, message requeued | deliveryTag={} | {}", tag, ex.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Replay failure detail | deliveryTag={}", tag, ex);
                }
                channel.basicNack(tag, false, true);
                return true;
            }
        } catch (Exception e) {
            logger.error("RabbitMQ error while draining retry queue | {}", e.getMessage(), e);
            return false;
        }
    }
}
