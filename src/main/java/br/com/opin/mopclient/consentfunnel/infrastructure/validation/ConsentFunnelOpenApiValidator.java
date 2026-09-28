package br.com.opin.mopclient.consentfunnel.infrastructure.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelViolation;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import br.com.opin.mopclient.validator.application.service.OpenApiSpecCompatibilityPatcher;
import br.com.opin.mopclient.validator.shared.util.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.validation.RequestValidator;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Valida o ingresso {@code POST /data-funil-consents} contra
 * {@code swagger/consent-funnel/consent-funnel.yml} (schema {@code ConsentCreated}).
 */
@Component
public class ConsentFunnelOpenApiValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentFunnelOpenApiValidator.class);
    private static final int MAX_VALIDATION_ERRORS = 100;

    static final String SPEC_CLASSPATH = "swagger/consent-funnel/consent-funnel.yml";
    static final String OPERATION_PATH = "/data-funil-consents";

    private final ObjectMapper objectMapper;
    private final OpenApi3 openApi;

    public ConsentFunnelOpenApiValidator(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.openApi = loadSpec();
    }

    public void validate(ConsentFunnelEventRequestDTO request) {
        JsonNode payload = parsePayload(request);

        Body requestBody = Body.from(payload);
        Request openApiRequest = new DefaultRequest.Builder(OPERATION_PATH, Request.Method.POST)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .build();

        RequestValidator validator = new RequestValidator(openApi);
        try {
            validator.validate(openApiRequest);
        } catch (ValidationException e) {
            List<ConsentFunnelViolation> violations = toViolations(e.results(), e.getMessage());
            if (violations.isEmpty()) {
                throw ConsentFunnelValidationException.single("body", e.getMessage(), "OPENAPI");
            }
            LOGGER.warn("Consent funnel OpenAPI validation failed | violations={}", violations.size());
            throw new ConsentFunnelValidationException("OpenAPI schema validation failed", violations);
        }
    }

    private OpenApi3 loadSpec() {
        ClassPathResource resource = new ClassPathResource(SPEC_CLASSPATH);
        try (InputStream inputStream = resource.getInputStream()) {
            OpenApi3 parsed = new OpenApi3Parser().parse(
                    FileUtils.inputStreamToFile(inputStream, "consent-funnel.yml"),
                    false);
            OpenApiSpecCompatibilityPatcher.patch(parsed);
            return parsed;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load consent funnel spec " + SPEC_CLASSPATH, e);
        }
    }

    private JsonNode parsePayload(ConsentFunnelEventRequestDTO request) {
        try {
            return objectMapper.valueToTree(request);
        } catch (Exception e) {
            throw ConsentFunnelValidationException.single(
                    "body",
                    "Invalid JSON payload: " + e.getMessage(),
                    "FUNNEL-SERIALIZE");
        }
    }

    private List<ConsentFunnelViolation> toViolations(ValidationResults validationResults, String fallbackMessage) {
        List<ConsentFunnelViolation> violations = new ArrayList<>();
        if (validationResults != null && validationResults.items() != null) {
            int count = 0;
            for (var item : validationResults.items()) {
                if (count >= MAX_VALIDATION_ERRORS) {
                    break;
                }
                if (OpenApiSpecCompatibilityPatcher.isStringNumericFormatFalsePositive(
                        item.code(), item.message())) {
                    continue;
                }
                violations.add(new ConsentFunnelViolation(
                        item.dataCrumbs() != null ? item.dataCrumbs() : "",
                        item.message(),
                        String.valueOf(item.code())));
                count++;
            }
        }
        if (violations.isEmpty() && fallbackMessage != null && !fallbackMessage.isBlank()) {
            violations.add(new ConsentFunnelViolation("", fallbackMessage, "OPENAPI"));
        }
        return violations;
    }
}
