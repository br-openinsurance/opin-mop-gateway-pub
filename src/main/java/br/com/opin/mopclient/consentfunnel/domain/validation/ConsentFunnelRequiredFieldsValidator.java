package br.com.opin.mopclient.consentfunnel.domain.validation;

import br.com.opin.mopclient.consentfunnel.domain.exception.ConsentFunnelValidationException;
import br.com.opin.mopclient.consentfunnel.domain.model.ConsentFunnelViolation;
import br.com.opin.mopclient.consentfunnel.interfaces.dto.ConsentFunnelEventRequestDTO;
import br.com.opin.mopclient.consentfunnel.shared.ConsentFunnelDateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Garante que os campos obrigatórios do evento não estejam ausentes nem em branco.
 */
public final class ConsentFunnelRequiredFieldsValidator {

    public static final String REQUIRED_CODE = "REQUIRED";

    private ConsentFunnelRequiredFieldsValidator() {
    }

    public static void validate(ConsentFunnelEventRequestDTO request) {
        List<ConsentFunnelViolation> violations = new ArrayList<>();
        requireText(violations, "consentId", request.getConsentId());
        requireText(violations, "step", request.getStep());
        requireText(violations, "origin", request.getOrigin());
        requireText(violations, "correlationId", request.getCorrelationId());
        requireText(violations, "timestamp", request.getTimestamp());
        requireTimestampFormat(violations, request.getTimestamp());
        requireText(violations, "clientSSId", request.getClientSSId());
        requireText(violations, "serverASId", request.getServerASId());
        requirePermissions(violations, request.getPermissions());

        if (!violations.isEmpty()) {
            throw new ConsentFunnelValidationException(
                    "Required fields must be filled",
                    violations);
        }
    }

    private static void requireText(List<ConsentFunnelViolation> violations, String field, String value) {
        if (value == null || value.isBlank()) {
            violations.add(new ConsentFunnelViolation(
                    field,
                    "The field " + field + " is required and cannot be empty",
                    REQUIRED_CODE));
        }
    }

    private static void requireTimestampFormat(List<ConsentFunnelViolation> violations, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!ConsentFunnelDateTime.isValid(value)) {
            violations.add(new ConsentFunnelViolation(
                    "timestamp",
                    "The field timestamp must match yyyy-MM-dd'T'HH:mm:ss",
                    REQUIRED_CODE));
        }
    }

    private static void requirePermissions(List<ConsentFunnelViolation> violations, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            violations.add(new ConsentFunnelViolation(
                    "permissions",
                    "The field permissions is required and cannot be empty",
                    REQUIRED_CODE));
            return;
        }
        for (int i = 0; i < permissions.size(); i++) {
            String item = permissions.get(i);
            if (item == null || item.isBlank()) {
                violations.add(new ConsentFunnelViolation(
                        "permissions[" + i + "]",
                        "The field permissions is required and cannot contain empty items",
                        REQUIRED_CODE));
            }
        }
    }
}
