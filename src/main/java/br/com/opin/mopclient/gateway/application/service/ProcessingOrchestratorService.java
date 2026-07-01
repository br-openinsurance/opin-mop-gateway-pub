package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.anonymization.application.usecase.AnonymizeDataUseCase;
import br.com.opin.mopclient.anonymization.interfaces.dto.AnonymizationConfigDTO;
import br.com.opin.mopclient.anonymization.interfaces.dto.message.MessageDTO;
import br.com.opin.mopclient.anonymization.interfaces.dto.message.MessageDTOBuilder;
import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.anonymization.infrastructure.mapper.MessagePayloadWrapper;
import br.com.opin.mopclient.anonymization.shared.exception.infrastructure.ConfigUnavailableException;
import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import br.com.opin.mopclient.anonymization.shared.util.JsonConverter;
import br.com.opin.mopclient.gateway.interfaces.dto.ServerResponseDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.ErrorResponseException;
import br.com.opin.mopclient.retry.ClientRetryUserMessages;
import br.com.opin.mopclient.retry.application.ClientRetryEnqueueService;
import br.com.opin.mopclient.retry.domain.ClientRetryFailureStage;
import br.com.opin.mopclient.retry.exception.ClientRetryEnqueuedException;
import br.com.opin.mopclient.security.SigningProperties;
import br.com.opin.mopclient.validator.application.service.OpenApiValidationService;
import br.com.opin.mopclient.validator.interfaces.dto.ValidationResponseDTO;
import br.com.opin.mopclient.validator.interfaces.dto.ValidationViolationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the synchronous request pipeline: validate → fetch anonymization config → anonymize →
 * build MessageDTO → wrap → POST to MOP (JWT body when signing is active). On MOP unavailability the
 * request can be enqueued to the retry queue unless {@code suppressRetryEnqueue} is set (used by the replay path).
 * Log entries follow the {@code [STEP n]} / {@code [STEP 6.x]} convention for traceability.
 */
@Service
public class ProcessingOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingOrchestratorService.class);

    private final OpenApiValidationService validationService;
    private final AnonymizeDataUseCase anonymizeDataUseCase;
    private final ExternalApiClient externalApiClient;
    private final MessagePayloadWrapper messagePayloadWrapper;
    private final ObjectMapper objectMapper;
    private final ClientRetryEnqueueService clientRetryEnqueueService;
    private final SigningProperties signingProperties;

    public ProcessingOrchestratorService(
            OpenApiValidationService validationService,
            AnonymizeDataUseCase anonymizeDataUseCase,
            ExternalApiClient externalApiClient,
            MessagePayloadWrapper messagePayloadWrapper,
            ObjectMapper objectMapper,
            ClientRetryEnqueueService clientRetryEnqueueService,
            SigningProperties signingProperties) {
        this.validationService = Objects.requireNonNull(validationService, "OpenApiValidationService cannot be null");
        this.anonymizeDataUseCase = Objects.requireNonNull(anonymizeDataUseCase, "AnonymizeDataUseCase cannot be null");
        this.externalApiClient = Objects.requireNonNull(externalApiClient, "ExternalApiClient cannot be null");
        this.messagePayloadWrapper = Objects.requireNonNull(messagePayloadWrapper, "MessagePayloadWrapper cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.clientRetryEnqueueService = Objects.requireNonNull(clientRetryEnqueueService, "ClientRetryEnqueueService cannot be null");
        this.signingProperties = Objects.requireNonNull(signingProperties, "SigningProperties cannot be null");
    }

    public ProcessingResult processRequest(String originalRequestBody, String normalizedPayload, RequestHeadersDTO headersDTO) {
        return processRequest(originalRequestBody, normalizedPayload, headersDTO, false);
    }

    /**
     * @param originalRequestBody exact HTTP body as received (preserved on enqueue for lossless replay)
     * @param normalizedPayload JSON after gateway parse/normalize (used for validation and anonymization)
     * @param suppressRetryEnqueue when {@code true} (replay from the retry queue), MOP failures are not re-enqueued
     */
    public ProcessingResult processRequest(
            String originalRequestBody,
            String normalizedPayload,
            RequestHeadersDTO headersDTO,
            boolean suppressRetryEnqueue) {
        String correlationId = headersDTO.getCorrelationId() != null ? headersDTO.getCorrelationId() : headersDTO.getMopReportid();
        MopReportidManager.setMopReportid(correlationId);
        
        try {
            logger.info("[STEP 0] Starting request processing | Correlation ID: {}", correlationId);

            logger.info("[STEP 1] Starting validation | Correlation ID: {}", correlationId);
            ValidationResponseDTO validationResult = performValidation(normalizedPayload, headersDTO);
            logger.info("[STEP 1] Validation completed | Correlation ID: {}", correlationId);

            logger.info("[STEP 2] Fetching anonymization configuration | Correlation ID: {}", correlationId);
            AnonymizationConfigDTO config;
            try {
                config = anonymizeDataUseCase.getConfiguration();
            } catch (ConfigUnavailableException e) {
                logger.warn(
                        "MOP unavailable (config fetch / anonymization-fields) | correlationId={} | summary={}",
                        correlationId,
                        shortMessage(e.getMessage()));
                if (logger.isDebugEnabled()) {
                    logger.debug("MOP unavailable (config) - detail and stack | correlationId={}", correlationId, e);
                }
                if (!suppressRetryEnqueue) {
                    clientRetryEnqueueService.enqueue(
                            ClientRetryFailureStage.ANONYMIZATION_CONFIG,
                            e.getMessage(),
                            originalRequestBody,
                            normalizedPayload,
                            headersDTO,
                            null);
                    throw new ClientRetryEnqueuedException(headersDTO, ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE);
                }
                throw e;
            }
            Set<String> anonymizedFields = config.getAnonymized() != null ? config.getAnonymized() : Set.of();
            Set<String> exposedFields = config.getExposed() != null ? config.getExposed() : Set.of();
            logger.info("[STEP 2] Configuration loaded | Anonymized: {} fields | Exposed: {} fields | Correlation ID: {}",
                    anonymizedFields.size(), exposedFields.size(), correlationId);

            logger.info("[STEP 3] Starting anonymization | Correlation ID: {}", correlationId);
            String anonymizedPayload = anonymizeDataUseCase.anonymizePayload(normalizedPayload, anonymizedFields, exposedFields);
            logger.info("[STEP 3] Anonymization completed | Correlation ID: {}", correlationId);

            logger.info("[STEP 4] Building MessageDTO | Correlation ID: {}", correlationId);
            List<Validation> validations = convertValidations(validationResult);
            MessageDTO messageDTO = buildMessageDTO(headersDTO, anonymizedFields, exposedFields, validations, anonymizedPayload);
            String messageJson = JsonConverter.toJson(messageDTO);

            logger.info("[STEP 5] Wrapping payload | Correlation ID: {}", correlationId);
            JsonNode payloadNode = parseJsonNode(anonymizedPayload);
            String wrappedJson = messagePayloadWrapper.wrap(messageJson, payloadNode);
            logger.info("[STEP 5] Payload wrapped | Correlation ID: {}", correlationId);

            logger.info(
                    "[STEP 6.1] Outbound: calling MOP process endpoint | wrapped payload length: {} chars | Correlation ID: {}",
                    wrappedJson.length(),
                    correlationId);
            ServerResponseDTO serverResponse;
            try {
                serverResponse = externalApiClient.sendJsonPayload(wrappedJson);
            } catch (CallNotPermittedException e) {
                logger.warn(
                        "MOP unavailable (circuit breaker open on /process) | correlationId={} | summary={}",
                        correlationId,
                        shortMessage(e.getMessage()));
                if (logger.isDebugEnabled()) {
                    logger.debug("MOP unavailable (circuit /process) - stack | correlationId={}", correlationId, e);
                }
                if (!suppressRetryEnqueue) {
                    clientRetryEnqueueService.enqueue(
                            ClientRetryFailureStage.PROCESS_ENDPOINT,
                            "Circuit breaker open for mopProcessEndpoint",
                            originalRequestBody,
                            normalizedPayload,
                            headersDTO,
                            wrappedJson);
                    throw new ClientRetryEnqueuedException(headersDTO, ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE);
                }
                throw e;
            } catch (ErrorResponseException e) {
                String errSummary = e.getDetails() != null ? e.getDetails() : e.getMessage();
                logger.warn(
                        "MOP unavailable (POST /process) | correlationId={} | summary={}",
                        correlationId,
                        shortMessage(errSummary));
                if (logger.isDebugEnabled()) {
                    logger.debug("MOP unavailable (/process) - detail and stack | correlationId={}", correlationId, e);
                }
                if (!suppressRetryEnqueue) {
                    clientRetryEnqueueService.enqueue(
                            ClientRetryFailureStage.PROCESS_ENDPOINT,
                            e.getDetails() != null ? e.getDetails() : e.getMessage(),
                            originalRequestBody,
                            normalizedPayload,
                            headersDTO,
                            wrappedJson);
                    throw new ClientRetryEnqueuedException(headersDTO, ClientRetryUserMessages.SUCCESS_AFTER_ENQUEUE);
                }
                throw e;
            }
            logger.info("[STEP 6.5] Outbound: MOP process call completed successfully | Correlation ID: {}", correlationId);

            return new ProcessingResult(wrappedJson, validations, serverResponse);

        } finally {
            MopReportidManager.clearMopReportid();
        }
    }

    private ValidationResponseDTO performValidation(String payload, RequestHeadersDTO headersDTO) {
        HttpHeaders httpHeaders = convertHeadersToHttpHeaders(headersDTO.getHeaders());
        String mopPath = OpenApiPathMatcher.extractOpenInsurancePath(headersDTO.getPath());
        return validationService.validate(
                payload,
                httpHeaders,
                mopPath,
                headersDTO.getOperation(),
                headersDTO.getHttpType(),
                headersDTO.getStatusCode());
    }

    private List<Validation> convertValidations(ValidationResponseDTO validatorResponse) {
        if (validatorResponse == null || validatorResponse.getValidationResult() == null) {
            return List.of();
        }

        var result = validatorResponse.getValidationResult();
        if (result.getValidations() == null || result.getValidations().isEmpty()) {
            return List.of();
        }

        return result.getValidations().stream()
                .map(this::convertValidationViolation)
                .collect(Collectors.toList());
    }

    private Validation convertValidationViolation(ValidationViolationDTO violation) {
        return Validation.builder()
                .violation(violation.getMessage())
                .code(violation.getCode())
                .severity(violation.getSeverity())
                .attribute(violation.getCrumbs())
                .build();
    }

    private MessageDTO buildMessageDTO(
            RequestHeadersDTO headersDTO,
            Set<String> anonymizedFields,
            Set<String> exposedFields,
            List<Validation> validations,
            String anonymizedPayload) {

        Map<String, String> headers = headersDTO.getHeaders() != null ? headersDTO.getHeaders() : Map.of();
        String host = extractHost(headers);
        String url = extractUrl(headers);
        String orgId = signingProperties.getOrgId() != null ? signingProperties.getOrgId() : "";

        return MessageDTOBuilder.buildFromHeaders(
                headersDTO,
                anonymizedFields,
                exposedFields,
                validations,
                anonymizedPayload,
                host,
                url,
                orgId);
    }

    private HttpHeaders convertHeadersToHttpHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        return httpHeaders;
    }

    private JsonNode parseJsonNode(String jsonString) {
        try {
            if (jsonString == null || jsonString.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            logger.warn("Failed to parse JSON, using empty object | Error: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String extractHost(Map<String, String> headers) {
        return headers.getOrDefault("host",
                headers.getOrDefault("Host",
                        headers.getOrDefault("X-Forwarded-Host", "unknown")));
    }

    private String extractUrl(Map<String, String> headers) {
        return headers.getOrDefault("url",
                headers.getOrDefault("Url",
                        headers.getOrDefault("X-Forwarded-Uri", "/")));
    }

    private static String shortMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "(no message)";
        }
        String one = msg.replace('\r', ' ').replace('\n', ' ').trim();
        int max = 200;
        return one.length() <= max ? one : one.substring(0, max) + "...";
    }
}
