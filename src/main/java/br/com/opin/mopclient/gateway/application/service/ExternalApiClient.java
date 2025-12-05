package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
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
 *   <li>{@link ErrorResponseException} with "Connection error" - for network/connection failures</li>
 *   <li>{@link ErrorResponseException} with "Client error" - for HTTP 4xx responses</li>
 *   <li>{@link ErrorResponseException} with "Server error" - for HTTP 5xx responses</li>
 *   <li>{@link ErrorResponseException} with "Request error" - for other REST client exceptions</li>
 * </ul>
 * <p>
 * <strong>Configuration:</strong>
 * <p>
 * The external API URL must be configured via the property:
 * <pre>{@code external.server.request.url=http://external-api.example.com/api}</pre>
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
 * @see RestTemplate
 * @see ErrorResponseException
 */
@Service
public class ExternalApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalApiClient.class);

    private final RestTemplate restTemplate;
    private final String externalRequestUrl;

    /**
     * Constructs a new instance of {@code ExternalApiClient}.
     *
     * @param restTemplate the RestTemplate instance for HTTP operations (must not be null)
     * @param externalRequestUrl the external API base URL (must not be null or blank)
     * @throws NullPointerException if restTemplate or externalRequestUrl is null
     * @throws IllegalStateException if externalRequestUrl is blank
     */
    public ExternalApiClient(
            RestTemplate restTemplate,
            @Value("${external.server.request.url}") String externalRequestUrl) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
        String url = Objects.requireNonNull(externalRequestUrl, 
                "external.server.request.url must be configured");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("external.server.request.url must be configured and cannot be blank");
        }
        this.externalRequestUrl = url;
    }

    /**
     * Logs service initialization with the configured external API URL.
     * This method is automatically called after dependency injection.
     */
    @PostConstruct
    private void logInitialization() {
        LOGGER.debug("ExternalApiClient initialized with URL: {}", externalRequestUrl);
    }

    /**
     * Sends a JSON payload via POST to the configured external URL.
     * <p>
     * Validates the payload before sending and handles exceptions appropriately.
     *
     * @param jsonPayload JSON string to be sent (must not be null or blank).
     * @throws IllegalArgumentException if the jsonPayload is null or blank.
     * @throws ErrorResponseException if an error occurs while sending the request.
     */
    public void sendJsonPayload(final String jsonPayload) {
        validatePayload(jsonPayload);
        
        LOGGER.debug("Preparing to send JSON payload to external service. Payload length: {}", 
                jsonPayload.length());

        try {
            HttpEntity<String> requestEntity = createJsonRequestEntity(jsonPayload);
            logRequestDetails(jsonPayload);

            Instant start = Instant.now();
            String url = Objects.requireNonNull(externalRequestUrl, "External request URL must not be null");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, 
                    requestEntity, 
                    String.class
            );
            Duration duration = Duration.between(start, Instant.now());

            logResponseDetails(response, duration);

        } catch (ResourceAccessException e) {
            handleResourceAccessException(e);
        } catch (HttpClientErrorException e) {
            handleHttpClientException(e);
        } catch (HttpServerErrorException e) {
            handleHttpServerException(e);
        } catch (RestClientException e) {
            handleRestClientException(e);
        }
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

    /**
     * Creates an HttpEntity with JSON content type headers.
     *
     * @param jsonPayload The JSON payload to wrap.
     * @return HttpEntity configured for JSON content.
     */
    private HttpEntity<String> createJsonRequestEntity(String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonPayload, headers);
    }

    /**
     * Logs request details for traceability.
     *
     * @param jsonPayload The payload being sent.
     */
    private void logRequestDetails(String jsonPayload) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("POST URL: {}", externalRequestUrl);
            LOGGER.debug("Request Payload: {}", jsonPayload);
        }
    }

    /**
     * Logs response details including status and duration.
     *
     * @param response The HTTP response received.
     * @param duration The duration of the request.
     */
    private void logResponseDetails(ResponseEntity<String> response, Duration duration) {
        LOGGER.info("Request completed successfully. Status: {} | Duration: {} ms", 
                response.getStatusCode(), 
                duration.toMillis());
        
        if (LOGGER.isDebugEnabled() && response.getBody() != null) {
            LOGGER.debug("Response Body: {}", response.getBody());
        }
    }

    /**
     * Handles ResourceAccessException (connection/timeout errors).
     */
    private void handleResourceAccessException(ResourceAccessException e) {
        String errorMessage = String.format("Unable to reach the server: %s", externalRequestUrl);
        LOGGER.error("Resource access error: {}", errorMessage, e);
        throw new ErrorResponseException(
                "Connection error",
                "The server could not be reached. Please check the hostname or network connectivity.",
                e
        );
    }

    /**
     * Handles HttpClientErrorException (4xx errors).
     */
    private void handleHttpClientException(HttpClientErrorException e) {
        String errorMessage = String.format("Client error when calling %s. Status: %s", 
                externalRequestUrl, e.getStatusCode());
        LOGGER.error("HTTP client error: {}", errorMessage, e);
        String responseBody = e.getResponseBodyAsString();
        String details = String.format("The server returned an error status: %s. Response: %s", 
                e.getStatusCode(), StringUtils.hasText(responseBody) ? responseBody : "N/A");
        throw new ErrorResponseException("Client error", details, e);
    }

    /**
     * Handles HttpServerErrorException (5xx errors).
     */
    private void handleHttpServerException(HttpServerErrorException e) {
        String errorMessage = String.format("Server error when calling %s. Status: %s", 
                externalRequestUrl, e.getStatusCode());
        LOGGER.error("HTTP server error: {}", errorMessage, e);
        String responseBody = e.getResponseBodyAsString();
        String details = String.format("The external server returned an error status: %s. Response: %s", 
                e.getStatusCode(), StringUtils.hasText(responseBody) ? responseBody : "N/A");
        throw new ErrorResponseException("Server error", details, e);
    }

    /**
     * Handles other RestClientException types.
     */
    private void handleRestClientException(RestClientException e) {
        String errorMessage = String.format("Unexpected error when calling %s", externalRequestUrl);
        LOGGER.error("Rest client error: {}", errorMessage, e);
        throw new ErrorResponseException(
                "Request error",
                "An unexpected error occurred while sending the request.",
                e
        );
    }
}

