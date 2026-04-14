package br.com.opin.mopclient.gateway.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

/**
 * Component that logs application properties on startup.
 * <p>
 * This component listens for ApplicationReadyEvent and logs important
 * configuration properties for debugging and verification purposes.
 * <p>
 * <strong>Security Note:</strong> This component does NOT log sensitive data
 * such as passwords, tokens, or credentials. Only safe configuration properties
 * are logged.
 */
@Component
public class ApplicationPropertiesLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationPropertiesLogger.class);
    private final Environment environment;

    /**
     * Constructor that injects the Environment dependency.
     *
     * @param environment The Spring Environment containing application properties.
     */
    public ApplicationPropertiesLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("=== Application configuration properties ===");
        logger.info("");

        // Server configuration
        logger.info("Server Configuration:");
        logProperty("server.port");
        logProperty("server.servlet.context-path");
        logger.info("");

        // Spring application configuration
        logger.info("Spring Application Configuration:");
        logProperty("spring.application.name");
        logProperty("spring.profiles.active");
        logger.info("");

        // RabbitMQ connection configuration (non-sensitive only)
        logger.info("RabbitMQ Connection Configuration:");
        logProperty("spring.rabbitmq.host");
        logProperty("spring.rabbitmq.port");
        logger.info("  spring.rabbitmq.username = [REDACTED]");
        logger.info("  spring.rabbitmq.password = [REDACTED]");
        logger.info("");

        // RabbitMQ listener configuration
        logger.info("RabbitMQ Listener Configuration:");
        logProperty("spring.rabbitmq.listener.simple.acknowledge-mode");
        logProperty("spring.rabbitmq.listener.simple.concurrency");
        logProperty("spring.rabbitmq.listener.simple.max-concurrency");
        logProperty("spring.rabbitmq.listener.simple.prefetch");
        logger.info("");

        // RabbitMQ queues configuration
        logger.info("RabbitMQ Queues Configuration:");
        logProperty("spring.rabbitmq.queues.validator.name");
        logProperty("spring.rabbitmq.queues.output.name");
        logProperty("mop.client.retry.queue");
        logger.info("");

        // RabbitMQ retry configuration
        logger.info("RabbitMQ Retry Configuration:");
        logProperty("spring.rabbitmq.retry.maxAttempts");
        logProperty("spring.rabbitmq.retry.backoff");
        logProperty("spring.rabbitmq.retry.enablesTransactionSupport");
        logger.info("");

        // External API configuration
        logger.info("External API Configuration:");
        logProperty("external.server.request.url");
        logger.info("");

        // Management endpoints configuration
        logger.info("Management Endpoints Configuration:");
        logProperty("management.endpoints.web.exposure.include");
        logProperty("management.endpoint.health.show-details");
        logger.info("");

        // Logging configuration
        logger.info("Logging Configuration:");
        logProperty("logging.level.root");
        logProperty("logging.level.br.com.opin.mopclient.gateway");
        logger.info("");

        logger.info("============================================");
    }

    /**
     * Logs a single property if it exists, or indicates it's not set.
     *
     * @param propertyKey The property key to log.
     */
    private void logProperty(String propertyKey) {
        String value = environment.getProperty(propertyKey);
        if (value != null) {
            logger.info("  {} = {}", propertyKey, value);
        } else {
            logger.info("  {} = [NOT SET]", propertyKey);
        }
    }
}

