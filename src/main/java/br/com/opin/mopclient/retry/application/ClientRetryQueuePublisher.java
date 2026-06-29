package br.com.opin.mopclient.retry.application;

import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Publishes {@link ClientRetryMessage} instances to RabbitMQ queues (retry and DLQ).
 */
@Service
public class ClientRetryQueuePublisher {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryQueuePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String retryQueueName;
    private final String dlqQueueName;

    public ClientRetryQueuePublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${mop.client.retry.queue}") String retryQueueName,
            @Value("${mop.client.retry.dlq.queue}") String dlqQueueName) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.retryQueueName = Objects.requireNonNull(retryQueueName, "mop.client.retry.queue must be configured");
        this.dlqQueueName = Objects.requireNonNull(dlqQueueName, "mop.client.retry.dlq.queue must be configured");
    }

    public void publishToRetryQueue(ClientRetryMessage message) {
        publish(retryQueueName, message, "[MOP retry]");
    }

    public void publishToDlq(ClientRetryMessage message) {
        publish(dlqQueueName, message, "[MOP retry DLQ]");
    }

    private void publish(String queueName, ClientRetryMessage message, String logPrefix) {
        Objects.requireNonNull(message, "message cannot be null");
        String correlationId = message.getCorrelationId() != null ? message.getCorrelationId() : "unknown";
        try {
            byte[] body = objectMapper.writeValueAsBytes(message);
            rabbitTemplate.convertAndSend("", queueName, body, m -> {
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                m.getMessageProperties().setContentType("application/json");
                return m;
            });
            logger.warn(
                    "{} Message published | queue={} | correlationId={} | attemptCount={} | dlqReason={}",
                    logPrefix,
                    queueName,
                    correlationId,
                    message.getAttemptCount(),
                    message.getDlqReason());
        } catch (Exception e) {
            logger.error(
                    "{} Failed to publish | queue={} | correlationId={} | {}",
                    logPrefix,
                    queueName,
                    correlationId,
                    e.getMessage(),
                    e);
            throw new IllegalStateException("Failed to publish message to queue " + queueName, e);
        }
    }
}
