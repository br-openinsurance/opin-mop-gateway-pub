package br.com.opin.mopclient.gateway.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * Declares the beans required to communicate with RabbitMQ.
 * <p>
 * - Creates a {@link CachingConnectionFactory} initialized with the configured credentials.<br>
 * - Exposes a reusable {@link RabbitTemplate} for the application layers.<br>
 * - Declares RabbitMQ queues for message consumption.<br>
 * - Enables RabbitMQ listener processing via {@link EnableRabbit}.
 * </p>
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);
    private static final String CONNECTION_NAME = "mop-client-gateway";

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.queues.output.name}")
    private String outputQueueName;

    /**
     * Declares the output queue for consuming messages from RabbitMQ.
     * The queue is durable to survive broker restarts.
     *
     * @return the configured Queue bean
     */
    @Bean
    public Queue outputQueue() {
        return new Queue(outputQueueName, true); // true = durable
    }

    /**
     * Builds the {@link CachingConnectionFactory} using the externalized properties.
     * Marked as @Primary to ensure it's used by all RabbitMQ components including listeners.
     */
    @Bean
    @Primary
    public CachingConnectionFactory connectionFactory() {
        String validatedHost = requireNonBlank(host, "spring.rabbitmq.host");
        String validatedUsername = requireNonBlank(username, "spring.rabbitmq.username");
        String validatedPassword = requireNonBlank(password, "spring.rabbitmq.password");
        int validatedPort = requirePositive(port, "spring.rabbitmq.port");

        try {
            CachingConnectionFactory factory = new CachingConnectionFactory(validatedHost, validatedPort);
            factory.setUsername(validatedUsername);
            factory.setPassword(validatedPassword);
            factory.setCacheMode(CacheMode.CHANNEL);
            factory.setChannelCacheSize(Math.max(factory.getChannelCacheSize(), 25));
            // Note: Connection cache size cannot be configured when cache mode is CHANNEL
            // The connection cache size is only applicable for CacheMode.CONNECTION
            factory.setConnectionNameStrategy(f -> CONNECTION_NAME);
            return factory;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to configure RabbitMQ connection", ex);
        }
    }

    /**
     * Exposes a standard {@link RabbitTemplate} backed by the configured factory.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        ConnectionFactory validatedFactory = Objects.requireNonNull(connectionFactory, "ConnectionFactory bean must be available");
        return new RabbitTemplate(validatedFactory);
    }

    /**
     * Configures the RabbitListenerContainerFactory to use the custom ConnectionFactory.
     * This ensures that @RabbitListener annotations use the correct connection settings.
     * <p>
     * The bean name 'rabbitListenerContainerFactory' is the default name expected by Spring Boot
     * for @RabbitListener annotations. This factory will be used automatically.
     *
     * @param connectionFactory the primary connection factory
     * @return configured SimpleRabbitListenerContainerFactory
     */
    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        logger.info("Creating RabbitListenerContainerFactory bean...");
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        factory.setAutoStartup(true);
        logger.info("RabbitListenerContainerFactory configured successfully. Queue: {}, Host: {}, Port: {}", 
                outputQueueName, host, port);
        return factory;
    }

    @NonNull
    private String requireNonBlank(String value, String propertyName) {
        String trimmed = Objects.requireNonNull(value, propertyName + " must be configured").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        return trimmed;
    }

    private int requirePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
        return value;
    }

}
