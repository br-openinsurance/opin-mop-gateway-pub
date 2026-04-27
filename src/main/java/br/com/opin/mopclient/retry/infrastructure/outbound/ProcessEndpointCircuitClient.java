package br.com.opin.mopclient.retry.infrastructure.outbound;

import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * HTTP POST to the MOP process URL, with the same error mapping as {@code ExternalApiClient},
 * protected by a circuit breaker.
 */
@Component
public class ProcessEndpointCircuitClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEndpointCircuitClient.class);

    private final RestTemplate restTemplate;

    public ProcessEndpointCircuitClient(RestTemplate restTemplate) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
    }

    @CircuitBreaker(name = "mopProcessEndpoint")
    public void postJson(String externalRequestUrl, String jsonPayload) {
        LOGGER.info(
                "[STEP 6.2] Outbound: POST via circuit breaker | URL: {} | JSON length before signing: {} chars | Correlation ID: {}",
                externalRequestUrl,
                jsonPayload.length(),
                correlationIdOrDash());

        try {
            HttpEntity<String> requestEntity = createJsonRequestEntity(jsonPayload);

            Instant start = Instant.now();
            ResponseEntity<String> response = restTemplate.postForEntity(
                    externalRequestUrl,
                    requestEntity,
                    String.class);
            Duration duration = Duration.between(start, Instant.now());

            LOGGER.info(
                    "[STEP 6.4] Outbound: HTTP response received | status: {} | durationMs: {} | Correlation ID: {}",
                    response.getStatusCode(),
                    duration.toMillis(),
                    correlationIdOrDash());

            if (LOGGER.isDebugEnabled()) {
                String body = response.getBody();
                LOGGER.debug(
                        "[STEP 6.4] Outbound: response body length={} | Correlation ID: {}",
                        body != null ? body.length() : 0,
                        correlationIdOrDash());
            }

        } catch (ResourceAccessException e) {
            handleResourceAccessException(externalRequestUrl, e);
        } catch (HttpClientErrorException e) {
            handleHttpClientException(externalRequestUrl, e);
        } catch (HttpServerErrorException e) {
            handleHttpServerException(externalRequestUrl, e);
        } catch (RestClientException e) {
            handleRestClientException(externalRequestUrl, e);
        }
    }

    private static String correlationIdOrDash() {
        String id = MopReportidManager.getMopReportid();
        return id != null && !id.isBlank() ? id : "n/a";
    }

    private static HttpEntity<String> createJsonRequestEntity(String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonPayload, headers);
    }

    private static void handleResourceAccessException(String externalRequestUrl, ResourceAccessException e) {
        String errorMessage = String.format("Unable to reach the server: %s", externalRequestUrl);
        LOGGER.error("Resource access error: {}", errorMessage, e);
        throw new ErrorResponseException(
                "Connection error",
                "The server could not be reached. Please check the hostname or network connectivity.",
                e
        );
    }

    private static void handleHttpClientException(String externalRequestUrl, HttpClientErrorException e) {
        String errorMessage = String.format("Client error when calling %s. Status: %s",
                externalRequestUrl, e.getStatusCode());
        LOGGER.error("HTTP client error: {}", errorMessage, e);
        String responseBody = e.getResponseBodyAsString();
        String details = String.format("The server returned an error status: %s. Response: %s",
                e.getStatusCode(), StringUtils.hasText(responseBody) ? responseBody : "N/A");
        throw new ErrorResponseException("Client error", details, e);
    }

    private static void handleHttpServerException(String externalRequestUrl, HttpServerErrorException e) {
        String errorMessage = String.format("Server error when calling %s. Status: %s",
                externalRequestUrl, e.getStatusCode());
        LOGGER.error("HTTP server error: {}", errorMessage, e);
        String responseBody = e.getResponseBodyAsString();
        String details = String.format("The external server returned an error status: %s. Response: %s",
                e.getStatusCode(), StringUtils.hasText(responseBody) ? responseBody : "N/A");
        throw new ErrorResponseException("Server error", details, e);
    }

    private static void handleRestClientException(String externalRequestUrl, RestClientException e) {
        String errorMessage = String.format("Unexpected error when calling %s", externalRequestUrl);
        LOGGER.error("Rest client error: {}", errorMessage, e);
        throw new ErrorResponseException(
                "Request error",
                "An unexpected error occurred while sending the request.",
                e
        );
    }
}
