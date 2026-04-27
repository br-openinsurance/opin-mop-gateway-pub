package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.retry.infrastructure.outbound.ProcessEndpointCircuitClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.Objects;

/**
 * Service responsible for sending HTTP requests to external APIs with comprehensive error handling
 * and performance monitoring.
 * <p>
 * This service provides an application layer abstraction for external HTTP communication,
 * ensuring input validation, proper error handling, request/response logging, and performance tracking.
 * It handles various HTTP error scenarios and provides detailed error information for troubleshooting.
 * <p>
 * <strong>Key Responsibilities:</strong>
 * <ul>
 *   <li>Validates JSON payload before sending</li>
 *   <li>Creates properly formatted HTTP requests with JSON content type</li>
 *   <li>Monitors request duration for performance analysis</li>
 *   <li>Handles different types of HTTP errors (4xx, 5xx, connection issues)</li>
 *   <li>Provides structured logging for request/response traceability</li>
 * </ul>
 * <p>
 * <strong>Error Handling:</strong>
 * <ul>
 *   <li>{@link IllegalArgumentException} - for invalid input (null or blank payload)</li>
 *   <li>{@link br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException} with "Connection error" - for network/connection failures</li>
 *   <li>{@link br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException} with "Client error" - for HTTP 4xx responses</li>
 *   <li>{@link br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException} with "Server error" - for HTTP 5xx responses</li>
 *   <li>{@link br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException} with "Request error" - for other REST client exceptions</li>
 * </ul>
 * <p>
 * <strong>Configuration:</strong>
 * <p>
 * The external API URL must be configured via the property:
 * <pre>{@code external.request.url=http://external-api.example.com/api}</pre>
 * (legacy fallback: {@code external.server.request.url})
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * @Autowired
 * private ExternalApiClient apiClient;
 *
 * String jsonPayload = "{\"key\":\"value\"}";
 * apiClient.sendJsonPayload(jsonPayload);
 * }</pre>
 * <p>
 * <strong>Performance Monitoring:</strong>
 * <p>
 * The service automatically tracks request duration and logs it at INFO level.
 * Response details are logged at DEBUG level when enabled.
 *
 * @author MOP Team
 * @since 1.0
 * @see org.springframework.web.client.RestTemplate
 * @see br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException
 */
@Service
public class ExternalApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalApiClient.class);

    private final ProcessEndpointCircuitClient processEndpointCircuitClient;
    private final String externalRequestUrl;

    /**
     * Constructs a new instance of {@code ExternalApiClient}.
     *
     * @param processEndpointCircuitClient client that performs the POST behind a circuit breaker
     * @param externalRequestUrl the external API base URL (must not be null or blank)
     * @throws NullPointerException if dependencies or externalRequestUrl is null
     * @throws IllegalStateException if externalRequestUrl is blank
     */
    public ExternalApiClient(
            ProcessEndpointCircuitClient processEndpointCircuitClient,
            @Value("${external.request.url:${external.server.request.url:}}") String externalRequestUrl) {
        this.processEndpointCircuitClient = Objects.requireNonNull(
                processEndpointCircuitClient, "ProcessEndpointCircuitClient cannot be null");
        String url = Objects.requireNonNull(externalRequestUrl,
                "external.request.url (or legacy external.server.request.url) must be configured");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("external.request.url (or legacy external.server.request.url) must be configured and cannot be blank");
        }
        this.externalRequestUrl = url;
    }

    /**
     * Logs service initialization with the configured external API URL.
     * This method is automatically called after dependency injection.
     */
    @PostConstruct
    private void logInitialization() {
        LOGGER.info("External API Client initialized successfully");
        LOGGER.info("  - External API URL: {}", externalRequestUrl);
    }

    /**
     * Sends a JSON payload via POST to the configured external URL.
     * <p>
     * Validates the payload before sending and handles exceptions appropriately.
     *
     * @param jsonPayload JSON string to be sent (must not be null or blank).
     * @throws IllegalArgumentException if the jsonPayload is null or blank.
     * @throws br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException if an error occurs while sending the request.
     */
    public void sendJsonPayload(final String jsonPayload) {
        validatePayload(jsonPayload);
        processEndpointCircuitClient.postJson(externalRequestUrl, jsonPayload);
    }

    /**
     * Validates that the JSON payload is not null or blank.
     *
     * @param jsonPayload The payload to validate.
     * @throws IllegalArgumentException if the payload is null or blank.
     */
    private void validatePayload(String jsonPayload) {
        if (!StringUtils.hasText(jsonPayload)) {
            throw new IllegalArgumentException("JSON payload cannot be null or blank");
        }
    }
}
