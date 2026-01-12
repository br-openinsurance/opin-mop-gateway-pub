package br.com.opin.mopclient.gateway.infrastructure.config;

import br.com.opin.mopclient.gateway.application.service.ExternalApiClient;
import br.com.opin.mopclient.gateway.infrastructure.adapter.RabbitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Componente responsável por verificar e logar o status de inicialização
 * da aplicação e todos os seus componentes quando a aplicação estiver pronta.
 * <p>
 * Este listener escuta o evento {@link ApplicationReadyEvent} e verifica:
 * - Conexão com RabbitMQ
 * - Configuração de filas
 * - Componentes de cache
 * - Serviços de API externa
 * - Demais componentes essenciais
 * <p>
 * Após verificar todos os componentes, registra um log indicando que
 * a aplicação está pronta para uso.
 */
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

    @Autowired(required = false)
    private ConnectionFactory connectionFactory;

    @Autowired(required = false)
    @Qualifier("outputQueue")
    private Queue outputQueue;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private RabbitListener rabbitListener;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private ExternalApiClient externalApiClient;

    @Value("${spring.rabbitmq.host:}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.queues.output.name:}")
    private String outputQueueName;

    @Value("${spring.application.name:mop-client-gateway}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("=================================================================");
        logger.info("  {} - Application Initialization", applicationName.toUpperCase());
        logger.info("=================================================================");
        logger.info("");

        boolean allComponentsReady = true;

        // Check RabbitMQ Connection Factory
        if (connectionFactory != null) {
            logger.info("✓ RabbitMQ Connection Factory: Initialized successfully");
            logger.info("  - Host: {}:{}", rabbitmqHost, rabbitmqPort);
        } else {
            logger.error("✗ RabbitMQ Connection Factory: NOT initialized");
            allComponentsReady = false;
        }

        // Check Output Queue
        if (outputQueue != null) {
            logger.info("✓ RabbitMQ Queue (Output Queue): Loaded successfully");
            logger.info("  - Queue name: {}", outputQueue.getName());
            logger.info("  - Durable: {}", outputQueue.isDurable());
        } else {
            logger.error("✗ RabbitMQ Queue (Output Queue): NOT loaded");
            allComponentsReady = false;
        }

        // Check RabbitMQ Template
        if (rabbitTemplate != null) {
            logger.info("✓ RabbitMQ Template: Configured successfully");
        } else {
            logger.error("✗ RabbitMQ Template: NOT configured");
            allComponentsReady = false;
        }

        // Check RabbitMQ Listener
        if (rabbitListener != null) {
            logger.info("✓ RabbitMQ Listener: Loaded and ready to receive messages");
            logger.info("  - Listening to queue: {}", outputQueueName);
        } else {
            logger.error("✗ RabbitMQ Listener: NOT loaded");
            allComponentsReady = false;
        }

        // Check Cache Manager
        if (cacheManager != null) {
            logger.info("✓ Cache Manager: Initialized successfully");
            logger.info("  - Available cache: {}", CacheConfig.RABBIT_MQ_CONFIG);
        } else {
            logger.error("✗ Cache Manager: NOT initialized");
            allComponentsReady = false;
        }

        // Check RestTemplate
        if (restTemplate != null) {
            logger.info("✓ RestTemplate: Configured successfully");
        } else {
            logger.error("✗ RestTemplate: NOT configured");
            allComponentsReady = false;
        }

        // Check ExternalApiClient
        if (externalApiClient != null) {
            logger.info("✓ External API Client: Loaded successfully");
        } else {
            logger.error("✗ External API Client: NOT loaded");
            allComponentsReady = false;
        }

        logger.info("");
        logger.info("=================================================================");

        if (allComponentsReady) {
            logger.info("  ✓ ALL COMPONENTS LOADED SUCCESSFULLY");
            logger.info("");
            logger.info("  Application Status: READY FOR USE");
            logger.info("  Server Port: {}", serverPort);
            logger.info("  Active Profile: {}", event.getApplicationContext().getEnvironment().getActiveProfiles().length > 0
                    ? String.join(", ", event.getApplicationContext().getEnvironment().getActiveProfiles())
                    : "default");
            logger.info("");
            logger.info("  The application is ready to receive requests and messages from the queue.");
            logger.info("=================================================================");
        } else {
            logger.error("  ✗ SOME COMPONENTS WERE NOT LOADED CORRECTLY");
            logger.error("  Please check the logs above to identify the issues.");
            logger.info("=================================================================");
        }

        logger.info("");
    }
}

