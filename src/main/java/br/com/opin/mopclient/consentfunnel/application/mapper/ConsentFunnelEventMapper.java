package br.com.opin.mopclient.consentfunnel.application.mapper;

import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelEvent;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelStep;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelAdditionalInfoDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte DTO de ingresso HTTP em modelo de domínio.
 */
public final class ConsentFunnelEventMapper {

    private ConsentFunnelEventMapper() {
    }

    public static ConsentFunnelEvent toDomain(ConsentFunnelEventRequestDTO request) {
        ConsentFunnelStep step = ConsentFunnelStep.fromValue(request.getStep()).orElse(null);
        return new ConsentFunnelEvent(
                request.getConsentId(),
                step,
                request.getOrigin(),
                request.getCorrelationId(),
                toAdditionalInfoMap(request.getAdditionalInfo()),
                request.getTimestamp(),
                request.getClientOrgId(),
                request.getClientSSId(),
                request.getServerOrgId(),
                request.getServerASId(),
                request.getPermissions());
    }

    private static Map<String, String> toAdditionalInfoMap(ConsentFunnelAdditionalInfoDTO info) {
        if (info == null) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        putIfPresent(map, "consent-user", info.getConsentUser());
        putIfPresent(map, "authentication-failure-reason", info.getAuthenticationFailureReason());
        putIfPresent(map, "user-redirected-back-status", info.getUserRedirectedBackStatus());
        putIfPresent(map, "token-kind", info.getTokenKind());
        putIfPresent(map, "rejected-by", info.getRejectedBy());
        putIfPresent(map, "revoked-by", info.getRevokedBy());
        putIfPresent(map, "expired-by", info.getExpiredBy());
        return Map.copyOf(map);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
