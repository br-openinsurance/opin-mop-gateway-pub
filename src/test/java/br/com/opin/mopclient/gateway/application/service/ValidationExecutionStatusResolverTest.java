package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ValidationExecutionStatusResolver")
class ValidationExecutionStatusResolverTest {

    @Test
    @DisplayName("returns SUCCESS when validations list is empty")
    void returnsSuccessWhenEmpty() {
        assertEquals("SUCCESS", ValidationExecutionStatusResolver.resolveStatus(List.of()));
        assertEquals("SUCCESS", ValidationExecutionStatusResolver.resolveStatus(null));
    }

    @Test
    @DisplayName("returns WARNING when only warnings are present")
    void returnsWarningWhenOnlyWarnings() {
        var validations = List.of(Validation.builder()
                .code("2001")
                .severity("WARNING")
                .attribute("meta.requestDateTime")
                .violation("Optional field 'requestDateTime' is missing.")
                .build());

        assertEquals("WARNING", ValidationExecutionStatusResolver.resolveStatus(validations));
    }

    @Test
    @DisplayName("returns ERROR when at least one validation is ERROR")
    void returnsErrorWhenErrorPresent() {
        var validations = List.of(
                Validation.builder()
                        .code("1025")
                        .severity("ERROR")
                        .attribute("x-fapi-interaction-id")
                        .violation("Value does not respect UUID format.")
                        .build(),
                Validation.builder()
                        .code("2001")
                        .severity("WARNING")
                        .attribute("meta.requestDateTime")
                        .violation("Optional field 'requestDateTime' is missing.")
                        .build());

        assertEquals("ERROR", ValidationExecutionStatusResolver.resolveStatus(validations));
    }

    @Test
    @DisplayName("returns ERROR for single ERROR validation")
    void returnsErrorForSingleError() {
        var validations = List.of(Validation.builder()
                .code("1026")
                .severity("ERROR")
                .attribute("body")
                .violation("Field 'data' is required.")
                .build());

        assertEquals("ERROR", ValidationExecutionStatusResolver.resolveStatus(validations));
    }
}
