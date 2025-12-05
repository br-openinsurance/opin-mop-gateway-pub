package br.com.opin.mopclient.gateway.application.usecase;

import br.com.opin.mopclient.gateway.infrastructure.adapter.RabbitMQMessagePublisher;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DataAnonymizerUseCase {
    private static final Logger logger = LoggerFactory.getLogger(DataAnonymizerUseCase.class);

    private final RabbitMQMessagePublisher messagePublisher;

    public DataAnonymizerUseCase(RabbitMQMessagePublisher messagePublisher) {
        this.messagePublisher = Objects.requireNonNull(messagePublisher, "RabbitMQMessagePublisher cannot be null.");
    }

    /**
     * Sends a message to RabbitMQ with retry and error handling.
     * Ensures the message is processed successfully with detailed logs.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(final String message) {
        logger.debug("Initiating message dispatch to RabbitMQ. Payload: {}", message);
        try {
            messagePublisher.sendMessage(message);
            logger.info("Message dispatched successfully to RabbitMQ.");
        } catch (Exception e) {
            String errorMessage = "Error dispatching message to RabbitMQ";
            logger.error("{} | Payload: {} | Exception: {}", errorMessage, message, e.getMessage(), e);
            throw new RabbitMQException(message, errorMessage, e);
        }
    }

    /**
     * Sends a message to RabbitMQ with headers.
     *
     * @param message    The message to be sent.
     * @param headersDTO The headers to include.
     */
    public void sendMessageWithHead(final String message, RequestHeadersDTO headersDTO) {
        logger.debug("Initiating message dispatch to RabbitMQ with headers. Payload: {}, Headers: {}", message, headersDTO);
        try {
            messagePublisher.sendMessageWithHead(message, headersDTO);
            logger.info("Message with headers dispatched successfully to RabbitMQ.");
        } catch (Exception e) {
            String errorMessage = "Error dispatching message with headers to RabbitMQ";
            logger.error("{} | Payload: {} | Headers: {} | Exception: {}", errorMessage, message, headersDTO, e.getMessage(), e);
            throw new RabbitMQException(message, errorMessage, e);
        }
    }
}