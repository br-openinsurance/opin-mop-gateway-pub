package br.com.opin.mopclient.validator.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenApiCurrentSpecRegistry")
class OpenApiCurrentSpecRegistryTest {

    private OpenApiCurrentSpecRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new OpenApiCurrentSpecRegistry();
        registry.loadAllSpecs();
    }

    @Test
    @DisplayName("loads only the matching spec file on resolve")
    void loadsSpecOnDemand() {
        OpenApiCurrentSpecRegistry lazyRegistry = new OpenApiCurrentSpecRegistry();
        assertEquals(0, lazyRegistry.loadedSpecFileCount());
        assertEquals(0, lazyRegistry.routeCount());
        assertTrue(lazyRegistry.indexedRouteCount() > 0, "lightweight index should be built on first resolve");

        var resolution = lazyRegistry.resolve("/open-insurance/customers/v2/personal/identifications");
        assertTrue(resolution.isPresent());
        assertEquals(1, lazyRegistry.loadedSpecFileCount(), "only customers.yaml should be parsed");
        assertEquals("customers.yaml", resolution.get().sourceFile());
        assertTrue(lazyRegistry.routeCount() > 0);
    }

    @Test
    @DisplayName("loads modular specs from swagger/current")
    void loadsModularSpecs() {
        assertTrue(registry.loadedSpecFileCount() > 0, "expected spec files loaded from swagger/current/");
        assertTrue(registry.routeCount() > 0, "expected routes indexed from swagger/current/");
        assertTrue(registry.failedSpecFiles().isEmpty(), "no spec file should fail to load: " + registry.failedSpecFiles());
    }

    @Test
    @DisplayName("resolves customers v2 personal identifications")
    void resolvesCustomersV2PersonalIdentifications() {
        var resolution = registry.resolve("/open-insurance/customers/v2/personal/identifications");
        assertTrue(resolution.isPresent());
        assertEquals("customers.yaml", resolution.get().sourceFile());
        assertEquals("/open-insurance/customers/v2", resolution.get().basePath());
        assertEquals("/personal/identifications", resolution.get().relativePath());
        assertEquals("/personal/identifications", resolution.get().pathTemplate());
    }

    @Test
    @DisplayName("resolves consents v3 collection path")
    void resolvesConsentsV3Collection() {
        var resolution = registry.resolve("/open-insurance/consents/v3/consents");
        assertTrue(resolution.isPresent());
        assertEquals("consents_v3.yaml", resolution.get().sourceFile());
        assertEquals("/consents", resolution.get().relativePath());
        assertEquals("/open-insurance/consents/v3", resolution.get().basePath());
    }

    @Test
    @DisplayName("resolves consents v3 by consentId")
    void resolvesConsentsV3ById() {
        var resolution = registry.resolve("/open-insurance/consents/v3/consents/urn:prudential:C1DD93123");
        assertTrue(resolution.isPresent());
        assertEquals("consents_v3.yaml", resolution.get().sourceFile());
        assertEquals("/consents/urn:prudential:C1DD93123", resolution.get().relativePath());
    }

    @Test
    @DisplayName("resolves business open data path")
    void resolvesBusinessOpenData() {
        var resolution = registry.resolve("/open-insurance/products-services/v1/business");
        assertTrue(resolution.isPresent());
        assertEquals("business.yaml", resolution.get().sourceFile());
        assertEquals("/business", resolution.get().relativePath());
    }

    @Test
    @DisplayName("resolves insurance-rural policy-info with concrete policyId")
    void resolvesInsuranceRuralPolicyInfoWithConcretePolicyId() {
        var resolution = registry.resolve(
                "/open-insurance/insurance-rural/v1/insurance-rural/12345788/policy-info");
        assertTrue(resolution.isPresent());
        assertEquals("insurance-rural.yaml", resolution.get().sourceFile());
        assertEquals("/insurance-rural/{policyId}/policy-info", resolution.get().pathTemplate());
        assertEquals("/insurance-rural/12345788/policy-info", resolution.get().relativePath());
    }

    @Test
    @DisplayName("returns empty for unknown path")
    void returnsEmptyForUnknownPath() {
        assertTrue(registry.resolve("/unknown/api/v99/resource").isEmpty());
    }

    @Test
    @DisplayName("resolves insurance-capitalization-title plans with single file parse")
    void resolvesInsuranceCapitalizationTitlePlans() {
        OpenApiCurrentSpecRegistry lazyRegistry = new OpenApiCurrentSpecRegistry();
        var resolution = lazyRegistry.resolve(
                "/open-insurance/insurance-capitalization-title/v1/insurance-capitalization-title/plans");
        assertTrue(resolution.isPresent());
        assertEquals("insurance-capitalization-title.yaml", resolution.get().sourceFile());
        assertEquals(OpenInsurancePhase.FASE_2, resolution.get().phase());
        assertEquals("/insurance-capitalization-title/plans", resolution.get().pathTemplate());
        assertEquals(1, lazyRegistry.loadedSpecFileCount());
    }

    @Test
    @DisplayName("resolves parameterized path without parsing unrelated specs")
    void resolvesParameterizedPathWithSingleParse() {
        OpenApiCurrentSpecRegistry lazyRegistry = new OpenApiCurrentSpecRegistry();
        var resolution = lazyRegistry.resolve(
                "/open-insurance/insurance-rural/v1/insurance-rural/12345788/policy-info");
        assertTrue(resolution.isPresent());
        assertEquals("insurance-rural.yaml", resolution.get().sourceFile());
        assertEquals(1, lazyRegistry.loadedSpecFileCount());
    }

    @Test
    @DisplayName("returns empty for incomplete capitalization-title base path")
    void returnsEmptyForIncompleteCapitalizationTitlePath() {
        OpenApiCurrentSpecRegistry lazyRegistry = new OpenApiCurrentSpecRegistry();
        assertTrue(lazyRegistry.resolve(
                "/open-insurance/insurance-capitalization-title/v1/insurance-capitalization-title").isEmpty());
        assertEquals(0, lazyRegistry.loadedSpecFileCount());
    }
}
