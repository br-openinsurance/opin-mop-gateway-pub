package br.com.opin.mopclient.consentfunnel.domain.model;

/**
 * Violação de regra de negócio do funil de consentimentos.
 */
public record ConsentFunnelViolation(String attribute, String message, String code, String severity) {

    public ConsentFunnelViolation(String attribute, String message, String code) {
        this(attribute, message, code, "ERROR");
    }
}
