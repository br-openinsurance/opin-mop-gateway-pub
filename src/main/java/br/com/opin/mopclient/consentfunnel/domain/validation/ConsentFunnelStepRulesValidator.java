package br.com.opin.mopclient.consentfunnel.domain.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelEvent;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Regras condicionais de {@code additionalInfo} por {@code step}, conforme spec PCM.
 */
public final class ConsentFunnelStepRulesValidator {

    private static final String RULE_CODE = "FUNNEL-STEP-RULE";

    private ConsentFunnelStepRulesValidator() {
    }

    public static void validate(ConsentFunnelEvent event) {
        if (event == null || event.step() == null) {
            throw ConsentFunnelValidationException.single("step", "Funnel step is required.", RULE_CODE);
        }

        Map<String, String> info = event.additionalInfo();
        List<ConsentFunnelViolation> violations = new ArrayList<>();

        switch (event.step()) {
            case CONSENT_CREATED -> requireKey(info, "consent-user", "consent-created", violations);
            case USER_AUTHENTICATION_FAILED -> requireKey(info, "authentication-failure-reason",
                    "user-authentication-failed", violations);
            case USER_REDIRECTED_BACK -> requireKey(info, "user-redirected-back-status",
                    "user-redirected-back", violations);
            case RESOURCE_ACCESSED -> requireKey(info, "token-kind", "resource-accessed", violations);
            case CONSENT_REJECTED -> requireKey(info, "rejected-by", "consent-rejected", violations);
            case CONSENT_REVOKED -> requireKey(info, "revoked-by", "consent-revoked", violations);
            case CONSENT_EXPIRED -> requireKey(info, "expired-by", "consent-expired", violations);
            default -> {
                // demais steps não exigem additionalInfo obrigatório nesta versão
            }
        }

        if (!violations.isEmpty()) {
            throw new ConsentFunnelValidationException("Funnel step conditional validation failed", violations);
        }
    }

    private static void requireKey(
            Map<String, String> info,
            String key,
            String stepName,
            List<ConsentFunnelViolation> violations) {
        if (info == null || !info.containsKey(key) || info.get(key) == null || info.get(key).isBlank()) {
            String attribute = "additionalInfo." + key;
            violations.add(new ConsentFunnelViolation(
                    attribute,
                    "Field '" + attribute + "' is required when step is '" + stepName + "'.",
                    RULE_CODE));
        }
    }
}
