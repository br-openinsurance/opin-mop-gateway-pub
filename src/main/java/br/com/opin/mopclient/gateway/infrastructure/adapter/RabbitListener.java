package br.com.opin.mopclient.gateway.infrastructure.adapter;

import br.com.opin.mopclient.gateway.application.service.ExternalApiClient;
import br.com.opin.mopclient.gateway.infrastructure.interceptor.RabbitMQCorrelationIdInterceptor;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQListener is a Spring component responsible for consuming messages from a RabbitMQ queue.
 * It processes each message asynchronously using a thread pool and forwards it to an external API.
 *
 * Features:
 * - Listens to a configured RabbitMQ queue.
 * - Uses a fixed thread pool to handle messages concurrently.
 * - Validates message content before processing.
 * Handles business and unexpected exceptions gracefully.
 * - Logs all operations in English for traceability and monitoring.
 *
 * Configuration:
 * - The queue name is injected via the property: `spring.rabbitmq.queues.output.name`.
 *
 * Example usage:
 * - A message arrives in the queue.
 * - The listener receives it and submits it to a thread pool.
 * - The message is validated and sent to an external API.
 * - Errors are logged and can be redirected to fallback mechanisms.
 */
@Component
@Lazy(false)
public class RabbitListener {

    private static final Logger logger = LoggerFactory.getLogger(RabbitListener.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Value("${spring.rabbitmq.queues.output.name}")
    private String queueName;

    @Autowired
    @Qualifier("outputQueue")
    private Queue outputQueue;

    @Autowired
    private ExternalApiClient apiClient;

    @Autowired
    private RabbitMQCorrelationIdInterceptor correlationIdInterceptor;

    public RabbitListener() {
        logger.info("Initializing RabbitMQ Listener...");
    }

    @PostConstruct
    public void init() {
        logger.info("RabbitMQ Listener configured successfully");
        logger.info("  - Configured queue: {}", queueName);
        logger.info("  - Queue bean: {}", outputQueue != null ? outputQueue.getName() : "not available");
        logger.info("  - Thread pool: 5 fixed threads");
        logger.info("RabbitMQ Listener ready to receive messages from queue: {}", queueName);
    }

    /**
     * Method triggered when a message is received from the configured RabbitMQ queue.
     * The message is submitted to the thread pool for asynchronous processing.
     * <p>
     * Extracts correlation ID from message headers and sets it in MDC for log traceability.
     *
     * @param message The RabbitMQ message with headers.
     */
    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "${spring.rabbitmq.queues.output.name}")
    public void receiveMessage(Message message) {
        logger.info("RabbitListener.receiveMessage() called - message received from queue: {}", queueName);
        executorService.submit(() -> processMessageSafely(message));
    }

    /**
     * Safely processes a message by validating its content and handling exceptions.
     * <p>
     * Sets correlation ID in MDC before processing and clears it after processing.
     *
     * @param message The RabbitMQ message to be processed.
     */
    private void processMessageSafely(Message message) {
        if (message == null || message.getBody() == null) {
            logger.warn("Received an empty or null message. Skipping processing.");
            return;
        }

        try {
            // Set correlation ID from message headers in MDC
            correlationIdInterceptor.beforeProcessMessage(message);

            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            if (messageBody.isBlank()) {
                logger.warn("Received an empty message body. Skipping processing.");
                return;
            }

            logger.info("Received message from queue: {}", queueName);
            logger.info("Starting message processing: {}", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));

            processMessage(messageBody);

        } catch (RabbitMQException e) {
            logger.error("Business exception during RabbitMQ processing: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during message processing: {}", e.getMessage(), e);
        } finally {
            // Always clear correlation ID from MDC after processing
            correlationIdInterceptor.afterProcessMessage();
        }
    }

    /**
     * Sends the message to an external API endpoint.
     * Throws a RabbitMQException in case of failure.
     *
     * @param messageBody The message body to be sent.
     */
    private void processMessage(final String messageBody) {
        try {
            logger.info("Sending message to Server API.");
            apiClient.sendJsonPayload(messageBody);
            logger.info("Message sent to external API successfully.");
        } catch (Exception e) {
            logger.error("Failed to send message to external API: {}", e.getMessage(), e);
            throw new RabbitMQException("Error while sending message to external API", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
}