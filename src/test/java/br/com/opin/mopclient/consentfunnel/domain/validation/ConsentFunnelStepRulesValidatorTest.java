package br.com.opin.mopclient.consentfunnel.domain.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelEvent;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ConsentFunnelStepRulesValidator Tests")
class ConsentFunnelStepRulesValidatorTest {

    @Test
    @DisplayName("should pass when consent-created includes consent-user")
    void shouldPassConsentCreatedWithConsentUser() {
        ConsentFunnelEvent event = baseEvent(
                ConsentFunnelStep.CONSENT_CREATED,
                Map.of("consent-user", "user"));

        assertDoesNotThrow(() -> ConsentFunnelStepRulesValidator.validate(event));
    }

    @Test
    @DisplayName("should fail when consent-created lacks consent-user")
    void shouldFailConsentCreatedWithoutConsentUser() {
        ConsentFunnelEvent event = baseEvent(ConsentFunnelStep.CONSENT_CREATED, Map.of());

        ConsentFunnelValidationException ex = assertThrows(
                ConsentFunnelValidationException.class,
                () -> ConsentFunnelStepRulesValidator.validate(event));

        assertEquals(1, ex.getViolations().size());
        assertEquals("additionalInfo.consent-user", ex.getViolations().get(0).attribute());
    }

    @Test
    @DisplayName("should fail when user-authentication-failed lacks authentication-failure-reason")
    void shouldFailAuthenticationFailedWithoutReason() {
        ConsentFunnelEvent event = baseEvent(ConsentFunnelStep.USER_AUTHENTICATION_FAILED, Map.of());

        ConsentFunnelValidationException ex = assertThrows(
                ConsentFunnelValidationException.class,
                () -> ConsentFunnelStepRulesValidator.validate(event));

        assertEquals("additionalInfo.authentication-failure-reason", ex.getViolations().get(0).attribute());
    }

    private static ConsentFunnelEvent baseEvent(ConsentFunnelStep step, Map<String, String> additionalInfo) {
        return new ConsentFunnelEvent(
                "urn:bancoex:C1DD33123",
                step,
                "CLIENT",
                "577869e5-4c63-4b19-9235-a18d22c80986",
                additionalInfo,
                "2022-11-07T17:26:32Z",
                "1fb79963-4bff-4204-9370-93aceb8a2f0d",
                "2a59c2a3-529f-41c6-97e3-77395e9951ca",
                "ff66b95a-d817-4fbe-949a-c5912e240189",
                "f8cd7b48-197d-419b-8680-f42226111b6f",
                null);
    }
}
