package br.com.opin.mopclient.retry.infrastructure.outbound;

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
        LOGGER.info("Sending JSON to external server | URL: {} | character length: {}",
                externalRequestUrl, jsonPayload.length());
        LOGGER.info("Request body (JSON) sent to server: {}", jsonPayload);

        try {
            HttpEntity<String> requestEntity = createJsonRequestEntity(jsonPayload);

            Instant start = Instant.now();
            ResponseEntity<String> response = restTemplate.postForEntity(
                    externalRequestUrl,
                    requestEntity,
                    String.class);
            Duration duration = Duration.between(start, Instant.now());

            LOGGER.info("Request completed successfully. Status: {} | Duration: {} ms",
                    response.getStatusCode(),
                    duration.toMillis());

            if (LOGGER.isDebugEnabled() && response.getBody() != null) {
                LOGGER.info("Response Body: {}", response.getBody());
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
