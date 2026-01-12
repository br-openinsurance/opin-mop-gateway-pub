package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.infrastructure.adapter.RabbitMQMessagePublisher;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service responsible for publishing messages to RabbitMQ queues with validation and error handling.
 * <p>
 * This service acts as an application layer abstraction for RabbitMQ messaging operations,
 * providing input validation, error handling, and logging before delegating to the infrastructure layer.
 * It ensures that only valid messages are sent to the message queue and provides appropriate
 * error handling for various failure scenarios.
 * <p>
 * <strong>Key Responsibilities:</strong>
 * <ul>
 *   <li>Validates message content before publishing</li>
 *   <li>Handles RabbitMQ-specific exceptions and converts generic exceptions</li>
 *   <li>Provides structured logging for message operations</li>
 *   <li>Supports both simple messages and messages with custom headers</li>
 * </ul>
 * <p>
 * <strong>Error Handling:</strong>
 * <ul>
 *   <li>Throws {@link IllegalArgumentException} for invalid input (null or blank messages)</li>
 *   <li>Throws {@link RabbitMQException} for messaging failures</li>
 *   <li>Preserves original exceptions as causes for better error traceability</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * @Autowired
 * private RabbitMQMessageService messageService;
 *
 * // Send simple message
 * messageService.sendMessage("{\"event\":\"user.created\"}");
 *
 * // Send message with headers
 * RequestHeadersDTO headers = RequestHeadersDTO.builder()
 *     .origin("gateway")
 *     .destination("processor")
 *     .path("/events")
 *     .operation("POST")
 *     .userID("user123")
 *     .build();
 * messageService.sendMessageWithHead("{\"data\":\"value\"}", headers);
 * }</pre>
 *
 * @author MOP Team
 * @since 1.0
 * @see RabbitMQMessagePublisher
 * @see RabbitMQException
 */
@Service
public class RabbitMQMessageService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageService.class);
    private final RabbitMQMessagePublisher messagePublisher;

    public RabbitMQMessageService(RabbitMQMessagePublisher messagePublisher) {
        this.messagePublisher = Objects.requireNonNull(
                messagePublisher, 
                "RabbitMQMessagePublisher cannot be null"
        );
    }

    /**
     * Sends a message to RabbitMQ with validation and error handling.
     * <p>
     * Validates the message before sending and handles exceptions appropriately.
     * Accepts empty messages (e.g., "{}") as valid payloads.
     *
     * @param message The message to be sent (must not be null, can be empty or "{}").
     * @throws IllegalArgumentException if the message is null.
     * @throws RabbitMQException if an error occurs while sending the message.
     */
    public void sendMessage(String message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        logger.debug("Preparing to send message to RabbitMQ. Message length: {}", message.length());
        
        try {
            messagePublisher.sendMessage(message);
            logger.info("Message sent successfully to RabbitMQ");
        } catch (RabbitMQException e) {
            logger.error("Failed to send message to RabbitMQ. Error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while sending message to RabbitMQ. Error: {}", 
                    e.getMessage(), e);
            throw new RabbitMQException(message, "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a message to RabbitMQ with headers, validation and error handling.
     * <p>
     * Validates both the message and headers before sending.
     * Accepts empty messages (e.g., "{}") as valid payloads.
     *
     * @param message The message to be sent (must not be null, can be empty or "{}").
     * @param headersDTO The headers to include (must not be null).
     * @throws IllegalArgumentException if the message is null or headersDTO is null.
     * @throws RabbitMQException if an error occurs while sending the message.
     */
    public void sendMessageWithHead(String message, RequestHeadersDTO headersDTO) {
        Objects.requireNonNull(message, "Message cannot be null");
        Objects.requireNonNull(headersDTO, "RequestHeadersDTO cannot be null");
        
        logger.debug("Preparing to send message with headers to RabbitMQ. Message length: {}, Origin: {}, Destination: {}", 
                message.length(),
                headersDTO.getOrigin(),
                headersDTO.getDestination());
        
        try {
            messagePublisher.sendMessageWithHead(message, headersDTO);
            logger.info("Message with headers sent successfully to RabbitMQ. Origin: {}, Destination: {}", 
                    headersDTO.getOrigin(), 
                    headersDTO.getDestination());
        } catch (RabbitMQException e) {
            logger.error("Failed to send message with headers to RabbitMQ. Error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while sending message with headers to RabbitMQ. Error: {}", 
                    e.getMessage(), e);
            throw new RabbitMQException(message, "Unexpected error: " + e.getMessage(), e);
        }
    }

}