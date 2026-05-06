package br.com.opin.mopclient.retry.infrastructure.outbound;

import br.com.opin.mopclient.anonymization.interfaces.dto.AnonymizationConfigDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * HTTP GET to {@code mop.endpoints.anonymization-config.url} protected by a circuit breaker.
 */
@Component
public class AnonymizationConfigCircuitClient {

    private final RestTemplate restTemplate;

    public AnonymizationConfigCircuitClient(RestTemplate restTemplate) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
    }

    @CircuitBreaker(name = "mopAnonymizationConfig")
    public AnonymizationConfigDTO getForObject(String endpointUrl) {
        return restTemplate.getForObject(endpointUrl, AnonymizationConfigDTO.class);
    }
}
