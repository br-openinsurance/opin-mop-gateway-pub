package br.com.opin.mopclient.consentfunnel.application.port.out;

import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;

/**
 * Envio do evento validado a {@code MOP_CONSENT_FUNNEL_METRICS_URL}.
 * JWT ou JSON em claro conforme {@code MOP_PAYLOAD_SIGNING_ENABLED}.
 */
public interface ConsentFunnelMetricsPort {

    void submit(String correlationId, ConsentFunnelEventRequestDTO event);
}
