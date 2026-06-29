package br.com.opin.mopclient.validator.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenApiValidationService")
class OpenApiValidationServiceTest {

    private OpenApiValidationService validationService;

    @BeforeEach
    void setUp() {
        OpenApiCurrentSpecRegistry registry = new OpenApiCurrentSpecRegistry();
        registry.loadAllSpecs();
        validationService = new OpenApiValidationService(registry);
    }

    @Test
    @DisplayName("returns validation violation when HTTP method is not defined for path")
    void returnsValidationViolationForWrongHttpMethod() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/customers/v2/personal/identifications",
                "POST",
                "server");

        var result = response.getValidationResult();
        assertNotNull(result);
        assertNotNull(result.getValidations());
        assertEquals(1, result.getValidations().size());
        assertEquals(
                "No POST operation for path '/open-insurance/customers/v2/personal/identifications'.",
                result.getValidations().get(0).getMessage());
        assertEquals("0", result.getValidations().get(0).getCode());
        assertEquals(
                "Validation failed: No POST operation for path '/open-insurance/customers/v2/personal/identifications'.",
                result.getDetailMessage());
    }

    @Test
    @DisplayName("resolves modular spec when path contains concrete policyId")
    void resolvesModularSpecWithConcretePolicyId() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/insurance-rural/v1/insurance-rural/12345788/policy-info",
                "GET",
                "client");

        var result = response.getValidationResult();
        assertNotNull(result);
        if (result.getValidations() != null) {
            assertTrue(result.getValidations().stream()
                    .noneMatch(v -> v.getMessage() != null
                            && v.getMessage().contains("Operation path not found")));
        }
    }

    @Test
    @DisplayName("resolves modular spec for customers v2 personal identifications GET")
    void resolvesModularSpecForCustomersV2PersonalIdentifications() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/customers/v2/personal/identifications",
                "GET",
                "client");

        var result = response.getValidationResult();
        assertNotNull(result);
        if (result.getValidations() != null) {
            assertTrue(result.getValidations().stream()
                    .noneMatch(v -> v.getMessage() != null
                            && v.getMessage().contains("Operation path not found")));
        }
    }

    @Test
    @DisplayName("returns validation violation when path is not found in any spec")
    void returnsValidationViolationForUnknownPath() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/unknown/v99/resource",
                "GET",
                "server");

        var result = response.getValidationResult();
        assertNotNull(result.getValidations());
        assertEquals(1, result.getValidations().size());
        assertEquals(
                "Operation path not found from URL '/open-insurance/unknown/v99/resource'.",
                result.getValidations().get(0).getMessage());
        assertEquals("NOT_FOUND", result.getValidations().get(0).getCode());
    }
}
