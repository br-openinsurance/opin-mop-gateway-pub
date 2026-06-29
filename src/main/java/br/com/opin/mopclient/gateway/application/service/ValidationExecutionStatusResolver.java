package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;

import java.util.Collections;
import java.util.List;

/**
 * Derives execution status from OpenAPI validation findings.
 */
public final class ValidationExecutionStatusResolver {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_ERROR = "ERROR";

    private ValidationExecutionStatusResolver() {
    }

    /**
     * @return {@value #STATUS_ERROR} if any validation has severity ERROR;
     *         else {@value #STATUS_WARNING} if any has severity WARNING;
     *         else {@value #STATUS_SUCCESS}
     */
    public static String resolveStatus(List<Validation> validations) {
        List<Validation> safeValidations = validations != null ? validations : List.of();
        if (safeValidations.isEmpty()) {
            return STATUS_SUCCESS;
        }
        if (hasSeverity(safeValidations, STATUS_ERROR)) {
            return STATUS_ERROR;
        }
        if (hasSeverity(safeValidations, STATUS_WARNING)) {
            return STATUS_WARNING;
        }
        return STATUS_SUCCESS;
    }

    public static List<Validation> preserveValidations(List<Validation> validations) {
        if (validations == null || validations.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(validations);
    }

    private static boolean hasSeverity(List<Validation> validations, String severity) {
        return validations.stream()
                .anyMatch(validation -> severity.equalsIgnoreCase(
                        validation != null ? validation.getSeverity() : null));
    }
}
