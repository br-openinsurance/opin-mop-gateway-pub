package br.com.opin.mopclient.anonymization.infrastructure.adapter.out.http;

import br.com.opin.mopclient.anonymization.application.port.out.repository.AnonymizationConfigRepository;
import br.com.opin.mopclient.anonymization.interfaces.dto.AnonymizationConfigDTO;
import br.com.opin.mopclient.anonymization.shared.exception.infrastructure.ConfigUnavailableException;
import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import br.com.opin.mopclient.retry.infrastructure.outbound.AnonymizationConfigCircuitClient;
import br.com.opin.mopclient.validator.infrastructure.config.InMemoryCacheManagerConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

/**
 * Adapter implementing AnonymizationConfigRepository using HTTP.
 */
@Component
public class RestTemplateConfigRepository implements AnonymizationConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfigRepository.class);

    private static final String ERROR_EMPTY_RESPONSE = "Failed to fetch config from server: empty (null) response. Endpoint: %s | MOP Report ID: %s";
    private static final String ERROR_HTTP_REQUEST_FAILED = "Failed to fetch config from server: HTTP request failed. Endpoint: %s | MOP Report ID: %s | Message: %s";
    private static final String ERROR_UNEXPECTED = "Failed to fetch config from server: unexpected error. Endpoint: %s | MOP Report ID: %s | Message: %s";

    private final AnonymizationConfigCircuitClient anonymizationConfigCircuitClient;

    @Value("${external.api.data-anonymization}")
    private String endpointUrl;

    public RestTemplateConfigRepository(AnonymizationConfigCircuitClient anonymizationConfigCircuitClient) {
        this.anonymizationConfigCircuitClient = Objects.requireNonNull(
                anonymizationConfigCircuitClient, "AnonymizationConfigCircuitClient cannot be null");
    }

    @PostConstruct
    public void init() {
        logger.info("External API Client configured | Endpoint URL: {}", endpointUrl);
    }

    @Override
    @Cacheable(value = InMemoryCacheManagerConfig.ANONYMIZATION_CONFIG, unless = "#result == null or (#result.anonymized == null or #result.anonymized.isEmpty()) and (#result.exposed == null or #result.exposed.isEmpty())")
    public AnonymizationConfigDTO findConfiguration() {
        String mopReportid = MopReportidManager.getMopReportid();

        try {
            AnonymizationConfigDTO response = anonymizationConfigCircuitClient.getForObject(endpointUrl);

            if (response == null) {
                String msg = String.format(ERROR_EMPTY_RESPONSE, endpointUrl, mopReportid);
                logger.error("[ERROR] {}", msg);
                throw new ConfigUnavailableException(msg);
            }

            boolean hasAnonymizedFields = response.getAnonymized() != null && !response.getAnonymized().isEmpty();
            boolean hasExposedFields = response.getExposed() != null && !response.getExposed().isEmpty();
            
            if (!hasAnonymizedFields && !hasExposedFields) {
                String msg = String.format(
                    "Invalid configuration returned from server: both anonymized and exposed fields are empty. Endpoint: %s | MOP Report ID: %s",
                    endpointUrl, mopReportid);
                logger.error("[ERROR] {}", msg);
                throw new ConfigUnavailableException(msg);
            }

            logger.info("[STEP 2] Configuration fetched | Anonymized: {} fields | Exposed: {} fields | MOP Report ID: {}",
                    response.getAnonymized() != null ? response.getAnonymized().size() : 0,
                    response.getExposed() != null ? response.getExposed().size() : 0,
                    mopReportid);

            return response;

        } catch (RestClientException e) {
            String msg = String.format(ERROR_HTTP_REQUEST_FAILED, endpointUrl, mopReportid, e.getMessage());
            logger.error("[ERROR] {}", msg, e);
            throw new ConfigUnavailableException(msg, e);
        } catch (ConfigUnavailableException e) {
            throw e;
        } catch (Exception e) {
            String msg = String.format(ERROR_UNEXPECTED, endpointUrl, mopReportid, e.getMessage());
            logger.error("[ERROR] {}", msg, e);
            throw new ConfigUnavailableException(msg, e);
        }
    }
}
