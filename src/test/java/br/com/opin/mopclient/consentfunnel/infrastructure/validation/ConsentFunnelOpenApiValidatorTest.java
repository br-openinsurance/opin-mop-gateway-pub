package br.com.opin.mopclient.consentfunnel.infrastructure.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelAdditionalInfoDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                .timestamp("2022-11-07T17:26:32Z")
                .clientOrgId("1fb79963-4bff-4204-9370-93aceb8a2f0d")
                .clientSSId("2a59c2a3-529f-41c6-97e3-77395e9951ca")
                .serverOrgId("ff66b95a-d817-4fbe-949a-c5912e240189")
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
    @DisplayName("should fail when a required org ID is missing")
    void shouldFailWhenRequiredIdMissing() {
        ConsentFunnelEventRequestDTO request = validRequest();
        request.setClientOrgId(null);

        assertThrows(ConsentFunnelValidationException.class, () -> validator.validate(request));
    }
}
