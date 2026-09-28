package br.com.opin.mopclient.consentfunnel.application.port.out;

import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;

/**
 * Envio do evento validado ao endpoint de métricas PCM/MOP (corpo assinado como {@code application/jwt}).
 */
public interface ConsentFunnelMetricsPort {

    void submit(String correlationId, ConsentFunnelEventRequestDTO event);
}
