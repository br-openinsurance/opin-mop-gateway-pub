package br.com.opin.mopclient.consentfunnel.domain.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConsentFunnelRequiredFieldsValidator Tests")
class ConsentFunnelRequiredFieldsValidatorTest {

    private static ConsentFunnelEventRequestDTO filled() {
        return ConsentFunnelEventRequestDTO.builder()
                .consentId("urn:bancoex:C1DD33123")
                .step("consent-created")
                .origin("CLIENT")
                .correlationId("577869e5-4c63-4b19-9235-a18d22c80986")
                .timestamp("2026-10-14T19:34:38")
                .clientSSId("2a59c2a3-529f-41c6-97e3-77395e9951ca")
                .serverASId("f8cd7b48-197d-419b-8680-f42226111b6f")
                .permissions(List.of("CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"))
                .build();
    }

    @Test
    @DisplayName("should pass when required fields are filled")
    void shouldPassWhenFilled() {
        assertDoesNotThrow(() -> ConsentFunnelRequiredFieldsValidator.validate(filled()));
    }

    @Test
    @DisplayName("should list every blank required field")
    void shouldListEveryBlankRequiredField() {
        ConsentFunnelEventRequestDTO request = ConsentFunnelEventRequestDTO.builder()
                .consentId("")
                .step(" ")
                .origin(null)
                .correlationId("")
                .timestamp(null)
                .clientSSId("  ")
                .serverASId("")
                .permissions(List.of())
                .build();

        ConsentFunnelValidationException ex = assertThrows(
                ConsentFunnelValidationException.class,
                () -> ConsentFunnelRequiredFieldsValidator.validate(request));

        assertEquals(8, ex.getViolations().size());
        assertTrue(ex.getViolations().stream().allMatch(v ->
                v.message().contains("is required")));
    }

    @Test
    @DisplayName("should reject timestamp with fraction or Z")
    void shouldRejectTimestampWithFractionOrZ() {
        ConsentFunnelEventRequestDTO request = filled();
        request.setTimestamp("2026-10-14T19:34:38.081366Z");

        ConsentFunnelValidationException ex = assertThrows(
                ConsentFunnelValidationException.class,
                () -> ConsentFunnelRequiredFieldsValidator.validate(request));

        assertEquals("timestamp", ex.getViolations().get(0).attribute());
        assertTrue(ex.getViolations().get(0).message().contains("yyyy-MM-dd'T'HH:mm:ss"));
    }
}
