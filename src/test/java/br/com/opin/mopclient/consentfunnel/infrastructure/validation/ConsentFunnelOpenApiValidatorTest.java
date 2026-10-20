package br.com.opin.mopclient.consentfunnel.infrastructure.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelAdditionalInfoDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ConsentFunnelOpenApiValidator Tests")
class ConsentFunnelOpenApiValidatorTest {

    private ConsentFunnelOpenApiValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConsentFunnelOpenApiValidator(new ObjectMapper());
    }

    private static ConsentFunnelEventRequestDTO validRequest() {
        return ConsentFunnelEventRequestDTO.builder()
                .consentId("urn:bancoex:C1DD33123")
                .step("consent-created")
                .origin("CLIENT")
                .correlationId("577869e5-4c63-4b19-9235-a18d22c80986")
                .additionalInfo(ConsentFunnelAdditionalInfoDTO.builder()
                        .consentUser("invalid-credentials")
                        .authenticationFailureReason("invalid-credentials")
                        .userRedirectedBackStatus("success")
                        .tokenKind("consent-token")
                        .rejectedBy("user")
                        .revokedBy("user")
                        .expiredBy("authorization-timeout")
                        .build())
                .timestamp("2026-10-14T19:34:38")
                .clientSSId("2a59c2a3-529f-41c6-97e3-77395e9951ca")
                .serverASId("f8cd7b48-197d-419b-8680-f42226111b6f")
                .permissions(List.of("CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"))
                .build();
    }

    @Test
    @DisplayName("should pass ConsentCreated payload from consent-funnel.yml")
    void shouldPassConsentCreatedPayload() {
        assertDoesNotThrow(() -> validator.validate(validRequest()));
    }

    @Test
    @DisplayName("should pass when additionalInfo is omitted")
    void shouldPassWithoutAdditionalInfo() {
        ConsentFunnelEventRequestDTO request = validRequest();
        request.setAdditionalInfo(null);

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("should fail when origin is missing")
    void shouldFailWhenOriginMissing() {
        ConsentFunnelEventRequestDTO request = validRequest();
        request.setOrigin(null);

        assertThrows(ConsentFunnelValidationException.class, () -> validator.validate(request));
    }

    @Test
    @DisplayName("should fail when a required AS/SS ID is missing")
    void shouldFailWhenRequiredIdMissing() {
        ConsentFunnelEventRequestDTO request = validRequest();
        request.setClientSSId(null);

        assertThrows(ConsentFunnelValidationException.class, () -> validator.validate(request));
    }

    @Test
    @DisplayName("should fail OpenAPI when extra root property is present")
    void shouldFailWhenUnknownRootPropertyPresent() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode) mapper.valueToTree(validRequest());
        node.put("orgId", "4b75db2e-a0c0-4359-a077-684e88fa695c");

        assertThrows(ConsentFunnelValidationException.class, () -> validator.validatePayload(node));
    }

    @Test
    @DisplayName("should reject unknown JSON properties on deserialize")
    void shouldRejectUnknownPropertiesOnDeserialize() {
        String json = """
                {
                  "consentId": "urn:bancoex:C1DD33123",
                  "step": "consent-created",
                  "origin": "CLIENT",
                  "correlationId": "577869e5-4c63-4b19-9235-a18d22c80986",
                  "timestamp": "2026-10-14T19:34:38",
                  "clientSSId": "2a59c2a3-529f-41c6-97e3-77395e9951ca",
                  "serverASId": "f8cd7b48-197d-419b-8680-f42226111b6f",
                  "permissions": ["CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"],
                  "orgId": "4b75db2e-a0c0-4359-a077-684e88fa695c"
                }
                """;

        Exception ex = assertThrows(Exception.class, () -> new ObjectMapper()
                .readValue(json, ConsentFunnelEventRequestDTO.class));
        assertNotNull(findFunnel(ex));
    }

    private static ConsentFunnelValidationException findFunnel(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConsentFunnelValidationException funnel) {
                return funnel;
            }
            current = current.getCause();
        }
        return null;
    }
}
