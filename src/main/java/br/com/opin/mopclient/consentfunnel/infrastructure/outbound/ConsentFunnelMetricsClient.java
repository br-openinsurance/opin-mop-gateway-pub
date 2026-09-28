package br.com.opin.mopclient.consentfunnel.infrastructure.outbound;

import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import br.com.opin.mopclient.consentfunnel.application.port.out.ConsentFunnelMetricsPort;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * POST do evento PCM para {@code mop.endpoints.consent-funnel-metrics.url}.
 * O {@link RestTemplate} aplica {@code PayloadSigningInterceptor} (JWT PS256 + {@code application/jwt}).
 */
@Component
public class ConsentFunnelMetricsClient implements ConsentFunnelMetricsPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentFunnelMetricsClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String metricsUrl;
    private final boolean enabled;

    public ConsentFunnelMetricsClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${mop.endpoints.consent-funnel-metrics.url:}") String metricsUrl,
            @Value("${mop.endpoints.consent-funnel-metrics.enabled:true}") boolean enabled) {
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.metricsUrl = metricsUrl != null ? metricsUrl.trim() : "";
        this.enabled = enabled;
    }

    @Override
    @CircuitBreaker(name = "mopConsentFunnelMetrics")
    public void submit(String correlationId, ConsentFunnelEventRequestDTO event) {
        Objects.requireNonNull(event, "event");
        if (!enabled) {
            LOGGER.debug("Consent funnel metrics delivery disabled | correlationId={}", correlationId);
            return;
        }
        if (!StringUtils.hasText(metricsUrl)) {
            throw new IllegalStateException(
                    "mop.endpoints.consent-funnel-metrics.url must be configured when delivery is enabled");
        }

        String previousMopReportId = MopReportidManager.getMopReportid();
        try {
            if (StringUtils.hasText(correlationId)) {
                MopReportidManager.setMopReportid(correlationId);
            }
            deliver(metricsUrl, serialize(event));
        } finally {
            if (previousMopReportId != null && !previousMopReportId.isBlank()) {
                MopReportidManager.setMopReportid(previousMopReportId);
            } else {
                MopReportidManager.clearMopReportid();
            }
        }
    }

    private void deliver(String url, String jsonPayload) {
        LOGGER.info(
                "[FUNNEL] Outbound: POST metrics | url={} | jsonLength={} | correlationId={}",
                url,
                jsonPayload.length(),
                correlationIdOrDash());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            Instant start = Instant.now();
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            Duration duration = Duration.between(start, Instant.now());

            LOGGER.info(
                    "[FUNNEL] Metrics response | status={} | durationMs={} | correlationId={}",
                    response.getStatusCode(),
                    duration.toMillis(),
                    correlationIdOrDash());
        } catch (ResourceAccessException e) {
            throw deliveryError("Connection error", "Unable to reach metrics endpoint: " + url, e);
        } catch (HttpClientErrorException e) {
            throw deliveryError(
                    "Client error",
                    "Metrics endpoint returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                    e);
        } catch (HttpServerErrorException e) {
            throw deliveryError(
                    "Server error",
                    "Metrics endpoint returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw deliveryError("Request error", "Failed to deliver consent funnel event to metrics", e);
        }
    }

    private String serialize(ConsentFunnelEventRequestDTO event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize consent funnel event", e);
        }
    }

    private static ErrorResponseException deliveryError(String error, String details, Throwable cause) {
        LOGGER.error("[FUNNEL] Metrics delivery failed | {} | {}", error, details, cause);
        return new ErrorResponseException(error, details, cause);
    }

    private static String correlationIdOrDash() {
        String id = MopReportidManager.getMopReportid();
        return id != null && !id.isBlank() ? id : "n/a";
    }
}
