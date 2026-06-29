package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ResponseBuilder")
class ResponseBuilderTest {

    private final ResponseBuilder responseBuilder = new ResponseBuilder();

    @Test
    @DisplayName("returns SUCCESS status when validations are empty")
    void returnsSuccessStatusWhenNoValidations() {
        var response = responseBuilder.buildSuccessResponse(
                "corr-1", "2026-01-01T00:00:00Z",
                "client", "server",
                "/open-insurance/consents/v3/consents", "POST");

        assertNotNull(response.getBody());
        assertEquals(ResponseBuilder.API_SUCCESS_BODY_MESSAGE, response.getBody().getMessage());
        assertNotNull(response.getBody().getValidations());
        assertEquals("SUCCESS", response.getBody().getValidations().getStatus());
        assertEquals(0, response.getBody().getValidations().getTotal());
        assertEquals(0, response.getBody().getValidations().getPending().size());
    }

    @Test
    @DisplayName("returns ERROR status when validations contain ERROR severity")
    void returnsErrorStatusWhenValidationErrorsPresent() {
        var validations = List.of(Validation.builder()
                .code("0")
                .severity("ERROR")
                .violation("Operation path not found from URL '/open-insurance/customers/v2/personal/identifications'.")
                .build());

        var response = responseBuilder.buildSuccessResponse(
                "corr-1", "2026-01-01T00:00:00Z",
                "client", "server",
                "/open-insurance/customers/v2/personal/identifications", "GET",
                validations);

        assertNotNull(response.getBody());
        assertEquals(ResponseBuilder.API_SUCCESS_BODY_MESSAGE, response.getBody().getMessage());
        assertNotNull(response.getBody().getValidations());
        assertEquals("ERROR", response.getBody().getValidations().getStatus());
        assertEquals(1, response.getBody().getValidations().getTotal());
        assertEquals(1, response.getBody().getValidations().getPending().size());
    }

    @Test
    @DisplayName("returns WARNING status when only warnings are present")
    void returnsWarningStatusWhenOnlyWarningsPresent() {
        var validations = List.of(Validation.builder()
                .code("2001")
                .severity("WARNING")
                .violation("Optional field missing.")
                .build());

        var response = responseBuilder.buildSuccessResponse(
                "corr-1", "2026-01-01T00:00:00Z",
                "client", "server",
                "/path", "GET",
                validations);

        assertNotNull(response.getBody());
        assertEquals("WARNING", response.getBody().getValidations().getStatus());
    }
}
