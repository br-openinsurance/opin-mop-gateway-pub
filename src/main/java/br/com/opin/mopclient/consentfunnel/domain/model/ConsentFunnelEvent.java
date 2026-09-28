package br.com.opin.mopclient.consentfunnel.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Modelo de domínio para evento do funil de consentimentos (schema PCM {@code eventBody}).
 */
public record ConsentFunnelEvent(
        String consentId,
        ConsentFunnelStep step,
        String origin,
        String correlationId,
        Map<String, String> additionalInfo,
        String timestamp,
        String clientOrgId,
        String clientSSId,
        String serverOrgId,
        String serverASId,
        List<String> permissions) {
}
