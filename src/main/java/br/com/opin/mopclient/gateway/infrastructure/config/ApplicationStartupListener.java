package br.com.opin.mopclient.gateway.infrastructure.config;

import br.com.opin.mopclient.gateway.application.service.ExternalApiClient;
import br.com.opin.mopclient.gateway.application.service.ProcessingOrchestratorService;
import br.com.opin.mopclient.validator.infrastructure.config.InMemoryCacheManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies and logs initialization status for the application and its main components
 * once the context is ready.
 * <p>
 * This listener handles {@link ApplicationReadyEvent} and checks:
 * - Cache components
 * - External API services
 * - Processing orchestrator
 * - Other essential components
 * <p>
 * After the checks, it logs whether the application is ready for use.
 */
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private ExternalApiClient externalApiClient;

    @Autowired(required = false)
    private ProcessingOrchestratorService orchestratorService;

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

        // Check Cache Manager
        if (cacheManager != null) {
            logger.info("✓ Cache Manager: Initialized successfully");
            logger.info("  - Available caches:");
            logger.info("    * {}", InMemoryCacheManagerConfig.OPEN_API_SPEC);
            logger.info("    * {}", InMemoryCacheManagerConfig.OPEN_API_SPEC);
            logger.info("    * {}", InMemoryCacheManagerConfig.NORMALIZED_ENDPOINTS);
            logger.info("    * {}", InMemoryCacheManagerConfig.ANONYMIZATION_CONFIG);
        } else {
            logger.error("✗ Cache Manager: NOT initialized");
            allComponentsReady = false;
        }

        // Check Processing Orchestrator Service
        if (orchestratorService != null) {
            logger.info("✓ Processing Orchestrator Service: Loaded successfully");
            logger.info("  - Unified flow: Validation → Anonymization → External API");
        } else {
            logger.error("✗ Processing Orchestrator Service: NOT loaded");
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
            logger.info("  The application is ready to receive requests and process them through the unified flow.");
            logger.info("=================================================================");
        } else {
            logger.error("  ✗ SOME COMPONENTS WERE NOT LOADED CORRECTLY");
            logger.error("  Please check the logs above to identify the issues.");
            logger.info("=================================================================");
        }

        logger.info("");
    }
}
