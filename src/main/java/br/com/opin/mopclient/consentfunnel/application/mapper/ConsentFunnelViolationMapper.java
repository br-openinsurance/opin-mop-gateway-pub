package br.com.opin.mopclient.consentfunnel.application.mapper;

import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelViolation;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelViolationDTO;

/**
 * Converte violações de domínio em DTO de resposta HTTP.
 */
public final class ConsentFunnelViolationMapper {

    private ConsentFunnelViolationMapper() {
    }

    public static ConsentFunnelViolationDTO toDto(ConsentFunnelViolation violation) {
        if (violation == null) {
            return null;
        }
        return ConsentFunnelViolationDTO.builder()
                .violation(violation.message())
                .code(violation.code())
                .severity(violation.severity())
                .attribute(violation.attribute())
                .build();
    }
}
