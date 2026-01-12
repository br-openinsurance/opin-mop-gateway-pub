
package br.com.opin.mopclient.gateway.infrastructure.adapter;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.dto.MessageHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import br.com.opin.mopclient.gateway.shared.util.MessageBuilderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component responsible for publishing messages to RabbitMQ queues.
 * <p>
 * Provides methods for sending simple messages and messages with custom headers.
 * Uses {@link RabbitTemplate} from Spring AMQP for communication with RabbitMQ.
 * </p>
 *
 * <p>Required configuration:
 * <ul>
 *   <li>Property {@code spring.rabbitmq.queues.validator.name} to define the queue name.</li>
 * </ul>
 * </p>
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * @Autowired
 * private RabbitMQMessagePublisher messagePublisher;
 *
 * messagePublisher.sendMessage("Simple message");
 *
 * RequestHeadersDTO headers = new RequestHeadersDTO();
 * headers.setOrigin("service-A");
 * headers.setDestination("service-B");
 * headers.setPath("/test");
 * headers.setOperation("create");
 * headers.setHeaders(Map.of("custom-key", "custom-value"));
 *
 * messagePublisher.sendMessageWithHead("{\"event\":\"test\"}", headers);
 * }
 * </pre>
 * </p>
 */
@Component
public class RabbitMQMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessagePublisher.class);

    /** RabbitMQ queue name, defined via configuration. */
    @Value("${spring.rabbitmq.queues.validator.name}")
    private String queueName;

    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructor that injects the {@link RabbitTemplate} instance.
     *
     * @param rabbitTemplate template instance for RabbitMQ communication
     */
    public RabbitMQMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends a simple message to the configured RabbitMQ queue.
     *
     * @param message message content to be sent
     * @throws RabbitMQException if an error occurs while sending the message
     */
    public void sendMessage(String message) {
        logger.debug("Sending message to RabbitMQ. Queue: {} | Payload: {}", queueName, message);
        try {
            rabbitTemplate.convertAndSend(queueName, message);
            logger.info("Message sent successfully to RabbitMQ. Queue: {}", queueName);
        } catch (Exception e) {
            String errorMessage = "Failed to send message to RabbitMQ";
            logger.error("{} | Queue: {} | Payload: {} | Exception: {}", errorMessage, queueName, message, e.getMessage(), e);
            throw new RabbitMQException(message, errorMessage, e);
        }
    }

    /**
     * Sends a message to RabbitMQ with custom headers.
     *
     * @param payload message content (must not be null or empty)
     * @param headersDTO object containing header information (origin, destination, operation, etc.)
     * @throws IllegalArgumentException if the payload is null or empty
     * @throws RabbitMQException if an error occurs while sending the message
     */
    public void sendMessageWithHead(String payload, RequestHeadersDTO headersDTO) {
        String headersSummary = formatHeadersSummary(headersDTO);
        logger.debug("Sending message with headers to RabbitMQ. Queue: {} | Payload length: {} | Headers: {}", 
                queueName, payload != null ? payload.length() : 0, headersSummary);
        try {
            Message message = MessageBuilderHelper.buildMessage(payload, headersDTO);
            MessageHeadersDTO headers = MessageBuilderHelper.createMessageHeadersFromDTO(headersDTO);

            rabbitTemplate.send("", queueName, message);
            logger.info("Message with headers sent successfully to RabbitMQ. Queue: {} | Origin: {} | Destination: {} | correlationID: {} | userID: {}", 
                    queueName, headers.getOrigin(), headers.getDestination(), headers.getCorrelationId(), headers.getUserID());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payload when sending message with headers. Queue: {} | Payload length: {} | Headers: {} | Exception: {}", 
                    queueName, payload != null ? payload.length() : 0, headersSummary, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            String errorMessage = "Failed to send message with headers to RabbitMQ";
            logger.error("{} | Queue: {} | Payload length: {} | Headers: {} | Exception: {}", 
                    errorMessage, queueName, payload != null ? payload.length() : 0, headersSummary, e.getMessage(), e);
            throw new RabbitMQException(payload, errorMessage, e);
        }
    }

    /**
     * Formats headers DTO into a readable string summary.
     *
     * @param headersDTO the headers DTO to format
     * @return formatted string with key header information
     */
    private String formatHeadersSummary(RequestHeadersDTO headersDTO) {
        if (headersDTO == null) {
            return "null";
        }
        return String.format("origin=%s, destination=%s, path=%s, operation=%s, userID=%s, applicationMode=%s, correlationID=%s, timestamp=%s",
                headersDTO.getOrigin(),
                headersDTO.getDestination(),
                headersDTO.getPath(),
                headersDTO.getOperation(),
                headersDTO.getUserID(),
                headersDTO.getApplicationMode(),
                headersDTO.getCorrelationID(),
                headersDTO.getTimestamp());
    }
}
