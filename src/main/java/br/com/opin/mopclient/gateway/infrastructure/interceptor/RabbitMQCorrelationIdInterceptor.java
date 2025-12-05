package br.com.opin.mopclient.gateway.infrastructure.interceptor;

import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;

/**
 * Interceptor for RabbitMQ messages that propagates correlation ID from message headers
 * to the MDC for automatic inclusion in all log statements during message processing.
 * <p>
 * This interceptor:
 * <ul>
 *   <li>Extracts correlation ID from RabbitMQ message headers</li>
 *   <li>Sets it in the MDC before message processing</li>
 *   <li>Generates a new traceable correlation ID if not present</li>
 *   <li>Clears the MDC after message processing</li>
 * </ul>
 * <p>
 * This ensures that all logs during message processing include the correlation ID,
 * enabling end-to-end traceability across distributed systems.
 *
 * @author MOP Team
 * @since 1.0
 */
@Component
public class RabbitMQCorrelationIdInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQCorrelationIdInterceptor.class);

    /**
     * Processes a RabbitMQ message and sets correlation ID in MDC.
     * <p>
     * This method should be called before processing a message to ensure
     * correlation ID is available in all logs during processing.
     *
     * @param message the RabbitMQ message
     */
    public void beforeProcessMessage(Message message) {
        if (message == null || message.getMessageProperties() == null) {
            CorrelationIdContext.setCorrelationId(null);
            return;
        }

        MessageProperties properties = message.getMessageProperties();
        Object correlationIdHeader = properties.getHeaders().get(CORRELATIONID);
        
        String correlationId = correlationIdHeader != null 
                ? correlationIdHeader.toString() 
                : null;
        
        CorrelationIdContext.setCorrelationId(correlationId);
        
        String actualCorrelationId = CorrelationIdContext.getCorrelationId();
        LOGGER.debug("Correlation ID set for RabbitMQ message processing: {}", actualCorrelationId);
    }

    /**
     * Clears correlation ID from MDC after message processing.
     * <p>
     * This method should be called after processing a message to prevent
     * correlation ID leakage between messages in thread pools.
     */
    public void afterProcessMessage() {
        CorrelationIdContext.clear();
    }
}

