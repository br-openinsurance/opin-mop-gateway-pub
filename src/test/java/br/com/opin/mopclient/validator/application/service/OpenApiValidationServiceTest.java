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
                "Request",
                null);

        var result = response.getValidationResult();
        assertNotNull(result);
        assertNotNull(result.getValidations());
        assertEquals(1, result.getValidations().size());
        assertEquals(
                "No POST operation for path '/open-insurance/customers/v2/personal/identifications'. "
                        + "Check that headers 'path' and 'operation' match the OpenAPI spec "
                        + "(e.g. POST /open-insurance/consents/v3/consents for create consent).",
                result.getValidations().get(0).getMessage());
        assertEquals("NOT_FOUND", result.getValidations().get(0).getCode());
        assertTrue(result.getDetailMessage().contains(
                "No POST operation for path '/open-insurance/customers/v2/personal/identifications'"));
    }

    @Test
    @DisplayName("resolves modular spec when path contains concrete policyId")
    void resolvesModularSpecWithConcretePolicyId() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/insurance-rural/v1/insurance-rural/12345788/policy-info",
                "GET",
                "Request",
                null);

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
                "Request",
                null);

        var result = response.getValidationResult();
        assertNotNull(result);
        if (result.getValidations() != null) {
            assertTrue(result.getValidations().stream()
                    .noneMatch(v -> v.getMessage() != null
                            && v.getMessage().contains("Operation path not found")));
        }
    }

    @Test
    @DisplayName("validates consents v3 POST request with minimal valid CreateConsent body")
    void validatesConsentsV3PostRequestWithValidBody() {
        String payload = """
                {
                  "data": {
                    "permissions": ["RESOURCES_READ"],
                    "loggedUser": {
                      "document": {
                        "identification": "11111111111",
                        "rel": "CPF"
                      }
                    },
                    "expirationDateTime": "2026-12-31T23:59:59Z"
                  }
                }
                """;
        var response = validationService.validate(
                payload,
                new HttpHeaders(),
                "/open-insurance/consents/v3/consents",
                "POST",
                "Request",
                null);

        var result = response.getValidationResult();
        assertNotNull(result);
        assertTrue(result.getValidations() == null || result.getValidations().isEmpty(),
                "valid CreateConsent body should pass OpenAPI validation");
    }

    @Test
    @DisplayName("validates consents v3 POST request body schema")
    void validatesConsentsV3PostRequest() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/consents/v3/consents",
                "POST",
                "Request",
                null);

        var result = response.getValidationResult();
        assertNotNull(result);
        assertNotNull(result.getValidations());
        assertFalse(result.getValidations().isEmpty(), "empty body must fail CreateConsent schema validation");
        assertTrue(result.getValidations().stream()
                .noneMatch(v -> v.getMessage() != null
                        && v.getMessage().contains("Operation path not found")));
    }

    @Test
    @DisplayName("lazy-loads consents v3 spec on first validation request")
    void lazyLoadsConsentsV3SpecOnDemand() {
        OpenApiCurrentSpecRegistry lazyRegistry = new OpenApiCurrentSpecRegistry();
        assertEquals(0, lazyRegistry.loadedSpecFileCount());

        OpenApiValidationService lazyService = new OpenApiValidationService(lazyRegistry);
        var response = lazyService.validate(
                """
                        {
                          "data": {
                            "permissions": ["RESOURCES_READ"],
                            "loggedUser": {
                              "document": {
                                "identification": "11111111111",
                                "rel": "CPF"
                              }
                            },
                            "expirationDateTime": "2026-12-31T23:59:59Z"
                          }
                        }
                        """,
                new HttpHeaders(),
                "/open-insurance/consents/v3/consents",
                "POST",
                "Request",
                null);

        assertTrue(lazyRegistry.loadedSpecFileCount() > 0);
        assertTrue(lazyRegistry.resolve("/open-insurance/consents/v3/consents").isPresent());
        var result = response.getValidationResult();
        assertNotNull(result);
        assertTrue(result.getValidations() == null || result.getValidations().isEmpty(),
                "lazy-loaded consents v3 route should validate successfully");
    }

    @Test
    @DisplayName("returns validation violation when path is not found in any spec")
    void returnsValidationViolationForUnknownPath() {
        var response = validationService.validate(
                "{}",
                new HttpHeaders(),
                "/open-insurance/unknown/v99/resource",
                "GET",
                "Response",
                "200");

        var result = response.getValidationResult();
        assertNotNull(result.getValidations());
        assertEquals(1, result.getValidations().size());
        assertEquals(
                "Operation path not found from URL '/open-insurance/unknown/v99/resource'.",
                result.getValidations().get(0).getMessage());
        assertEquals("NOT_FOUND", result.getValidations().get(0).getCode());
    }
}
