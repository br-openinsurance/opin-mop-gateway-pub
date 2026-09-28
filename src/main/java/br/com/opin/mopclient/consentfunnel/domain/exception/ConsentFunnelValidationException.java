package br.com.opin.mopclient.consentfunnel.domain.exception;

import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelViolation;

import java.util.Collections;
import java.util.List;

/**
 * Exceção de domínio para violações de contrato do funil de consentimentos.
 */
public class ConsentFunnelValidationException extends RuntimeException {

    private final List<ConsentFunnelViolation> violations;

    public ConsentFunnelValidationException(String message, List<ConsentFunnelViolation> violations) {
        super(message);
        this.violations = violations != null ? List.copyOf(violations) : List.of();
    }

    public List<ConsentFunnelViolation> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public static ConsentFunnelValidationException single(String field, String message, String code) {
        return new ConsentFunnelValidationException(
                message,
                List.of(new ConsentFunnelViolation(field, message, code)));
    }
}
