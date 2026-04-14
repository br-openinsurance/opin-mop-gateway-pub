package br.com.opin.mopclient.anonymization.application.usecase;

import br.com.opin.mopclient.anonymization.application.port.out.repository.AnonymizationConfigRepository;
import br.com.opin.mopclient.anonymization.domain.service.DataEncryptor;
import br.com.opin.mopclient.anonymization.domain.service.JsonAnonymizer;
import br.com.opin.mopclient.anonymization.shared.exception.domain.BusinessException;
import br.com.opin.mopclient.anonymization.interfaces.dto.AnonymizationConfigDTO;
import br.com.opin.mopclient.anonymization.shared.exception.infrastructure.ConfigUnavailableException;
import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import br.com.opin.mopclient.validator.infrastructure.config.InMemoryCacheManagerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Use case for data anonymization operations.
 */
@Service
public class AnonymizeDataUseCase {

    private
    static final Logger logger = LoggerFactory.getLogger(AnonymizeDataUseCase.class);
    private static final String EMPTY_JSON = "{}";

    private final JsonAnonymizer jsonAnonymizer;
    private final DataEncryptor dataEncryptor;
    private final AnonymizationConfigRepository configRepository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public AnonymizeDataUseCase(
            JsonAnonymizer jsonAnonymizer,
            DataEncryptor dataEncryptor,
            AnonymizationConfigRepository configRepository,
            CacheManager cacheManager,
            ObjectMapper objectMapper) {
        this.jsonAnonymizer = Objects.requireNonNull(jsonAnonymizer, "JsonAnonymizer cannot be null");
        this.dataEncryptor = Objects.requireNonNull(dataEncryptor, "DataEncryptor cannot be null");
        this.configRepository = Objects.requireNonNull(configRepository, "AnonymizationConfigRepository cannot be null");
        this.cacheManager = Objects.requireNonNull(cacheManager, "CacheManager cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    }

    public AnonymizationConfigDTO getConfiguration() {
        String mopReportid = MopReportidManager.getMopReportid();
        AnonymizationConfigDTO config = configRepository.findConfiguration();

        if (config == null) {
            throw new ConfigUnavailableException("Anonymization config returned null | MOP Report ID: " + mopReportid);
        }

        boolean hasAnonymizedFields = config.getAnonymized() != null && !config.getAnonymized().isEmpty();
        boolean hasExposedFields = config.getExposed() != null && !config.getExposed().isEmpty();
        
        if (!hasAnonymizedFields && !hasExposedFields) {
            String errorMsg = String.format(
                "Invalid configuration detected (possibly from cache): both anonymized and exposed fields are empty. " +
                "This configuration will be rejected. MOP Report ID: %s", mopReportid);
            logger.error("[ERROR] Invalid configuration detected. Invalidating cache | MOP Report ID: {}", mopReportid);
            
            try {
                if (cacheManager != null) {
                    var cache = cacheManager.getCache(InMemoryCacheManagerConfig.ANONYMIZATION_CONFIG);
                    if (cache != null) {
                        cache.clear();
                    }
                }
            } catch (Exception e) {
                logger.warn("[WARN] Failed to invalidate cache | MOP Report ID: {} | Error: {}", mopReportid, e.getMessage());
            }
            
            throw new ConfigUnavailableException(errorMsg);
        }

        return config;
    }

    public String encryptData(String input) {
        Objects.requireNonNull(input, "Input cannot be null");
        if (input.isBlank()) {
            throw new BusinessException("Input cannot be empty");
        }
        return dataEncryptor.encrypt(input);
    }

    public String anonymizePayload(String jsonInput, Set<String> fieldsToAnonymize, Set<String> fieldsToExpose) {
        String mopReportid = MopReportidManager.getMopReportid();
        if (jsonInput == null || jsonInput.isBlank()) {
            logger.warn("[WARN] Empty JSON input. Returning empty object | MOP Report ID: {}", mopReportid);
            return EMPTY_JSON;
        }

        Set<String> anonymizedFields = fieldsToAnonymize != null ? fieldsToAnonymize : Collections.emptySet();
        Set<String> exposedFields = fieldsToExpose != null ? fieldsToExpose : Collections.emptySet();

        logger.info("=================================================================================");
        logger.info("[INPUT] JSON to be anonymized | MOP Report ID: {}", mopReportid);
        logger.info("---------------------------------------------------------------------------------");
        
        String formattedJsonInput = jsonInput;
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonInput);
            formattedJsonInput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            logger.warn("[WARN] Could not format input JSON, using raw format | MOP Report ID: {}", mopReportid);
        }
        
        logger.info("JSON INPUT (pretty-printed):\n{}", formattedJsonInput);
        logger.info("FIELDS TO ANONYMIZE (fieldsToAnonymize): {}", anonymizedFields);
        logger.info("FIELDS TO EXPOSE (fieldsToExpose): {}", exposedFields);
        logger.info("=================================================================================");

        try {
            String anonymizedResult = jsonAnonymizer.anonymize(jsonInput, anonymizedFields, exposedFields);
            
            logger.info("=================================================================================");
            logger.info("[OUTPUT] JSON after anonymization | MOP Report ID: {}", mopReportid);
            logger.info("---------------------------------------------------------------------------------");
            
            String formattedJsonOutput = anonymizedResult;
            try {
                if (anonymizedResult != null && !anonymizedResult.trim().isEmpty() && !EMPTY_JSON.equals(anonymizedResult.trim())) {
                    JsonNode jsonNode = objectMapper.readTree(anonymizedResult);
                    formattedJsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                }
            } catch (Exception e) {
                logger.warn("[WARN] Could not format output JSON, using raw format | MOP Report ID: {} | Error: {}", mopReportid, e.getMessage());
            }
            
            logger.info("ANONYMIZED JSON (pretty-printed):\n{}", formattedJsonOutput);
            logger.info("=================================================================================");
            
            return anonymizedResult;
        } catch (IllegalArgumentException e) {
            logger.error("[ERROR] Invalid argument during anonymization | MOP Report ID: {} | Error: {}",
                    mopReportid, e.getMessage(), e);
            logger.error("[ERROR] ORIGINAL JSON (before error): {}", jsonInput);
            return EMPTY_JSON;
        } catch (Exception e) {
            logger.error("[ERROR] Unexpected error during anonymization | MOP Report ID: {} | Error: {}",
                    mopReportid, e.getMessage(), e);
            logger.error("[ERROR] ORIGINAL JSON (before error): {}", jsonInput);
            return EMPTY_JSON;
        }
    }
}
