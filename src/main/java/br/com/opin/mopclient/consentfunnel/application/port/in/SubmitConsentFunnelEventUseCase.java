package br.com.opin.mopclient.consentfunnel.application.port.in;

import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventAcceptedResponseDTO;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;

/**
 * Caso de uso de ingresso de eventos do funil de consentimentos.
 */
public interface SubmitConsentFunnelEventUseCase {

    ConsentFunnelEventAcceptedResponseDTO submit(ConsentFunnelEventRequestDTO request);
}
