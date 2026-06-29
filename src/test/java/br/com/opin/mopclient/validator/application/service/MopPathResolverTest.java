package br.com.opin.mopclient.validator.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MopPathResolver")
class MopPathResolverTest {

    private MopPathResolver resolver;

    @BeforeEach
    void setUp() {
        OpenApiCurrentSpecRegistry registry = new OpenApiCurrentSpecRegistry();
        registry.loadAllSpecs();
        resolver = new MopPathResolver(registry);
    }

    @Test
    @DisplayName("resolves transmitter URL with policyId parameter")
    void resolvesInsuranceAutoPolicyInfoFromUrl() {
        MopPathResolution resolution = resolver.resolveFromTransmitterUrl(
                "https://api.seguro.com.br/open-insurance/insurance-auto/v1/insurance-auto/abc-123/policy-info");

        assertTrue(resolution.matched());
        assertEquals(
                "/open-insurance/insurance-auto/v1/insurance-auto/abc-123/policy-info",
                resolution.mopPath());
        assertEquals("insurance-auto.yaml", resolution.sourceFile());
        assertEquals("/insurance-auto/{policyId}/policy-info", resolution.pathTemplate());
        assertEquals("/insurance-auto/abc-123/policy-info", resolution.relativePath());
    }

    @Test
    @DisplayName("builds MOP path from template and path parameters")
    void buildsFromPathTemplateWithParameters() {
        MopPathResolution resolution = resolver.buildFromPathTemplate(
                "/open-insurance/products-services/v1",
                "/home-insurance/commercializationArea/{commercializationArea}",
                Map.of("commercializationArea", "SP"));

        assertTrue(resolution.matched());
        assertEquals(
                "/open-insurance/products-services/v1/home-insurance/commercializationArea/SP",
                resolution.mopPath());
        assertEquals("home-insurance.yaml", resolution.sourceFile());
    }

    @Test
    @DisplayName("resolves insurance-rural policy-info with concrete policyId")
    void resolvesInsuranceRuralPolicyInfoFromUrl() {
        MopPathResolution resolution = resolver.resolveFromTransmitterUrl(
                "open-insurance/insurance-rural/v1/insurance-rural/12345788/policy-info");

        assertTrue(resolution.matched());
        assertEquals(
                "/open-insurance/insurance-rural/v1/insurance-rural/12345788/policy-info",
                resolution.mopPath());
        assertEquals("insurance-rural.yaml", resolution.sourceFile());
        assertEquals("/insurance-rural/{policyId}/policy-info", resolution.pathTemplate());
        assertEquals("/insurance-rural/12345788/policy-info", resolution.relativePath());
    }

    @Test
    @DisplayName("resolves customers v2 personal identifications from transmitter URL")
    void resolvesCustomersV2PersonalIdentificationsFromUrl() {
        MopPathResolution resolution = resolver.resolveFromTransmitterUrl(
                "https://api.seguro.com.br/open-insurance/customers/v2/personal/identifications");

        assertTrue(resolution.matched());
        assertEquals(
                "/open-insurance/customers/v2/personal/identifications",
                resolution.mopPath());
        assertEquals("customers.yaml", resolution.sourceFile());
        assertEquals("/personal/identifications", resolution.pathTemplate());
        assertEquals("/personal/identifications", resolution.relativePath());
    }

    @Test
    @DisplayName("returns unmatched resolution when path is unknown")
    void returnsUnmatchedForUnknownPath() {
        MopPathResolution resolution = resolver.resolveFromTransmitterUrl(
                "/open-insurance/unknown/v99/resource");

        assertFalse(resolution.matched());
        assertEquals("/open-insurance/unknown/v99/resource", resolution.mopPath());
        assertNull(resolution.sourceFile());
    }
}
