package br.com.opin.mopclient.consentfunnel.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Etapas do funil de consentimentos conforme {@code consent-funnel-ingestion.yaml} (schema {@code eventBody}).
 */
public enum ConsentFunnelStep {

    CONSENT_CREATED("consent-created"),
    USER_REDIRECTED("user-redirected"),
    USER_AUTHENTICATION_FAILED("user-authentication-failed"),
    USER_AUTHENTICATED("user-authenticated"),
    CONSENT_AUTHORIZED("consent-authorized"),
    CONSENT_REJECTED("consent-rejected"),
    AUTHORIZATION_CODE_CREATED("authorization-code-created"),
    USER_REDIRECTED_BACK("user-redirected-back"),
    CONSENT_TOKEN_GENERATED("consent-token-generated"),
    CONSENT_TOKEN_RECEIVED("consent-token-received"),
    REFRESH_TOKEN_USED("refresh-token-used"),
    RESOURCE_ACCESSED("resource-accessed"),
    CONSENT_REVOKED("consent-revoked"),
    CONSENT_EXPIRED("consent-expired");

    private final String value;

    ConsentFunnelStep(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<ConsentFunnelStep> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(step -> step.value.equals(value))
                .findFirst();
    }
}
