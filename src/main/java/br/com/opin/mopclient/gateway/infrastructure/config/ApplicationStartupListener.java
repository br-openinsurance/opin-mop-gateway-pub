package br.com.opin.mopclient.gateway.infrastructure.config;

import br.com.opin.mopclient.gateway.application.service.ExternalApiClient;
import br.com.opin.mopclient.gateway.application.service.ProcessingOrchestratorService;
import br.com.opin.mopclient.retry.application.MopServerAvailabilityProbe;
import br.com.opin.mopclient.validator.application.service.OpenApiCurrentSpecRegistry;
import br.com.opin.mopclient.validator.infrastructure.config.InMemoryCacheManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Single structured startup report after the context is ready (troubleshooting-oriented).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

    private static final int RABBIT_AMQP_PORT = 5672;
    private static final int RABBIT_MANAGEMENT_PORT = 15672;

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private ExternalApiClient externalApiClient;

    @Autowired(required = false)
    private ProcessingOrchestratorService orchestratorService;

    @Autowired(required = false)
    private MopServerAvailabilityProbe mopServerAvailabilityProbe;

    @Autowired(required = false)
    private OpenApiCurrentSpecRegistry openApiCurrentSpecRegistry;

    @Value("${spring.application.name:mop-client-gateway}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${cache.app-config.ttl-seconds:1800}")
    private long appConfigTtlSeconds;

    @Value("${cache.normalized-endpoints.ttl-seconds:300}")
    private long normalizedEndpointsTtlSeconds;

    @Value("${cache.max-size:10000}")
    private long maxNormalizedEndpointsSize;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> warnings = new ArrayList<>();
        List<String> notReadyReasons = new ArrayList<>();

        String profiles = activeProfilesSummary(event);
        String path = normalizedContextPath();

        logger.info("[STARTUP]");
        logger.info("- application = {}", applicationName);
        logger.info("- profile = {}", profiles);
        logger.info("- port = {}", serverPort);
        logger.info("- context-path = {}", path.isEmpty() ? "/" : path);

        String processUrlRaw = environment.getProperty("mop.endpoints.process.url");
        String configUrlRaw = environment.getProperty("mop.endpoints.anonymization-config.url");
        StartupHttpUrlSupport.Assessment processUrl = StartupHttpUrlSupport.assess(processUrlRaw);
        StartupHttpUrlSupport.Assessment configUrl = StartupHttpUrlSupport.assess(configUrlRaw);

        if (cacheManager == null) {
            notReadyReasons.add("CacheManager bean missing");
        }
        if (orchestratorService == null) {
            notReadyReasons.add("ProcessingOrchestratorService bean missing");
        }
        if (restTemplate == null) {
            notReadyReasons.add("RestTemplate bean missing");
        }
        if (externalApiClient == null) {
            notReadyReasons.add("ExternalApiClient bean missing");
        }
        if (!processUrl.valid()) {
            notReadyReasons.add("mop.endpoints.process.url — " + processUrl.statusLine());
        }
        if (!configUrl.valid()) {
            notReadyReasons.add("mop.endpoints.anonymization-config.url — " + configUrl.statusLine());
        }

        boolean probeEnabled = mopServerAvailabilityProbe != null && mopServerAvailabilityProbe.isProbeEnabled();
        boolean mopReachable =
                !probeEnabled || (mopServerAvailabilityProbe != null && mopServerAvailabilityProbe.isServerAvailable());

        if (probeEnabled && configUrl.valid() && !mopReachable) {
            warnings.add(
                    "MOP anonymization-config probe: last check did not succeed (metrics: mop.server.available)");
        }

        String rabbitHost = environment.getProperty("spring.rabbitmq.host");
        String rabbitPortStr = environment.getProperty("spring.rabbitmq.port");
        Integer rabbitPort = parsePort(rabbitPortStr);
        String rabbitStatus = rabbitBrokerStatus(rabbitHost, rabbitPort, warnings);

        logger.info("[DEPENDENCIES]");
        logger.info("- MOP process API | {} | url = {}", processUrl.statusLine(), nullToDash(processUrl.url()));
        logger.info(
                "- MOP anonymization config API | {} | probe = {} | url = {}",
                configUrl.statusLine(),
                probeStatusLabel(mopServerAvailabilityProbe, probeEnabled, configUrl.valid(), mopReachable),
                nullToDash(configUrl.url()));
        logger.info(
                "- RabbitMQ | {} | host = {} | port = {}",
                rabbitStatus,
                nullToDash(rabbitHost),
                nullToDash(rabbitPortStr));

        logger.info("[OPENAPI]");
        if (openApiCurrentSpecRegistry != null) {
            openApiCurrentSpecRegistry.loadAllSpecs();
            logger.info(
                    "- swagger/current | indexed = {} | discovered = {} | loaded = {} | parsed routes = {}",
                    openApiCurrentSpecRegistry.indexedRouteCount(),
                    openApiCurrentSpecRegistry.discoveredSpecFileCount(),
                    openApiCurrentSpecRegistry.loadedSpecFileCount(),
                    openApiCurrentSpecRegistry.routeCount());
            if (!openApiCurrentSpecRegistry.failedSpecFiles().isEmpty()) {
                warnings.add("OpenAPI spec load failures: " + openApiCurrentSpecRegistry.failedSpecFiles());
            }
            if (openApiCurrentSpecRegistry.resolve("/open-insurance/consents/v3/consents").isEmpty()) {
                warnings.add(
                        "OpenAPI route /open-insurance/consents/v3/consents is not resolvable — "
                                + "consent API validation will fail until swagger/current specs load correctly");
            } else {
                logger.info("- swagger/current | consents v3 POST route = OK");
            }
        } else {
            warnings.add("OpenApiCurrentSpecRegistry bean missing — modular OpenAPI validation unavailable");
            logger.info("- swagger/current | (registry bean missing)");
        }

        logger.info("[CACHE]");
        logger.info(
                "- {} | ttl = {}s | maxSize = {}",
                InMemoryCacheManagerConfig.NORMALIZED_ENDPOINTS,
                normalizedEndpointsTtlSeconds,
                maxNormalizedEndpointsSize);
        logger.info(
                "- {} | ttl = {}s | maxSize = {}",
                InMemoryCacheManagerConfig.ANONYMIZATION_CONFIG,
                appConfigTtlSeconds,
                100);
        if (cacheManager != null) {
            var names = cacheManager.getCacheNames().stream().sorted().toList();
            logger.info("- registered caches ({}): {}", names.size(), String.join(", ", names));
        } else {
            logger.info("- registered caches: (none — CacheManager missing)");
        }

        logger.info("[OBSERVABILITY]");
        logger.info(
                "- actuator include = {}",
                nullToDash(environment.getProperty("management.endpoints.web.exposure.include")));
        logger.info(
                "- health details = {}",
                nullToDash(environment.getProperty("management.endpoint.health.show-details")));
        String healthUrl = "http://localhost:" + serverPort + path + "/actuator/health";
        logger.info("- health endpoint = {}", healthUrl);
        logger.info(
                "- logging | root = {} | gateway = {}",
                nullToDash(environment.getProperty("logging.level.root")),
                nullToDash(environment.getProperty("logging.level.br.com.opin.mopclient.gateway")));

        logger.info("[WARNINGS]");
        if (warnings.isEmpty()) {
            logger.info("- (none)");
        } else {
            for (String w : warnings) {
                logger.warn("- {}", w);
            }
        }

        logFinalStatus(notReadyReasons, warnings);

        logger.info("Ingress (example) POST http://localhost:{}{}/data", serverPort, path);
    }

    private void logFinalStatus(List<String> notReadyReasons, List<String> warnings) {
        logger.info("[FINAL STATUS]");
        if (!notReadyReasons.isEmpty()) {
            logger.error("- NOT_READY");
            for (String r : notReadyReasons) {
                logger.error("  - {}", r);
            }
            return;
        }
        if (!warnings.isEmpty()) {
            logger.warn("- READY_DEGRADED — see [WARNINGS] above");
            return;
        }
        logger.info("- READY");
    }

    private static String probeStatusLabel(
            MopServerAvailabilityProbe probe,
            boolean probeEnabled,
            boolean configUrlOk,
            boolean mopReachable) {
        if (probe == null) {
            return "N/A (probe bean absent)";
        }
        if (!probeEnabled) {
            return "SKIPPED (mop.server.availability.enabled=false)";
        }
        if (!configUrlOk) {
            return "N/A (invalid URL)";
        }
        return mopReachable ? "UP" : "DOWN";
    }

    private String rabbitBrokerStatus(String host, Integer port, List<String> warnings) {
        if (host == null || host.isBlank()) {
            warnings.add("spring.rabbitmq.host is not set — verify broker configuration");
            return "UNKNOWN";
        }
        if (port != null && port == RABBIT_MANAGEMENT_PORT) {
            warnings.add(
                    "RabbitMQ port "
                            + port
                            + " is usually the management UI; AMQP is typically "
                            + RABBIT_AMQP_PORT
                            + " — verify spring.rabbitmq.port / env");
            return "CONFIGURED (suspect port)";
        }
        if (port != null && port != RABBIT_AMQP_PORT) {
            warnings.add(
                    "RabbitMQ port "
                            + port
                            + " is not the usual AMQP port "
                            + RABBIT_AMQP_PORT
                            + " — confirm broker settings");
            return "CONFIGURED";
        }
        return "CONFIGURED";
    }

    private static Integer parsePort(String portStr) {
        if (portStr == null || portStr.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizedContextPath() {
        if (contextPath == null || contextPath.isBlank()) {
            return "";
        }
        return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String activeProfilesSummary(ApplicationReadyEvent event) {
        String[] profiles = event.getApplicationContext().getEnvironment().getActiveProfiles();
        return profiles.length > 0 ? String.join(", ", profiles) : "default";
    }
}
