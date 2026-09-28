package br.com.opin.mopclient.validator.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenApiSpecPhaseCatalog")
class OpenApiSpecPhaseCatalogTest {

    private final OpenApiCurrentSpecRegistry registry = new OpenApiCurrentSpecRegistry();

    @Test
    @DisplayName("maps Fase 1 products-services path")
    void mapsFase1ProductsServices() {
        assertEquals(
                OpenInsurancePhase.FASE_1,
                registry.phaseForPath("/open-insurance/products-services/v1/business"));
    }

    @Test
    @DisplayName("maps Fase 2 insurance-capitalization-title plans")
    void mapsFase2CapitalizationTitle() {
        assertEquals(
                OpenInsurancePhase.FASE_2,
                registry.phaseForPath(
                        "/open-insurance/insurance-capitalization-title/v1/insurance-capitalization-title/plans"));
    }

    @Test
    @DisplayName("maps Fase 3 quote-auto path")
    void mapsFase3QuoteAuto() {
        assertEquals(
                OpenInsurancePhase.FASE_3,
                registry.phaseForPath("/open-insurance/quote-auto/v1/lead/request"));
    }

    @Test
    @DisplayName("maps Fases 2 e 3 consents path")
    void mapsFase2And3Consents() {
        assertEquals(
                OpenInsurancePhase.FASE_2_AND_3,
                registry.phaseForPath("/open-insurance/consents/v3/consents"));
    }

    @Test
    @DisplayName("resolve PCM consent funnel client-event path")
    void resolvePcmConsentFunnelClientEvent() {
        registry.loadAllSpecs();
        var resolution = registry.resolve("/event-api/v1/client-event");
        assertTrue(resolution.isPresent(), "consent-funnel-ingestion.yaml must be indexed");
        assertEquals(
                OpenApiSpecPhaseCatalog.CONSENT_FUNNEL_INGESTION_SPEC,
                resolution.orElseThrow().sourceFile());
    }

    @Test
    @DisplayName("resolve includes phase on spec resolution")
    void resolveIncludesPhase() {
        registry.loadAllSpecs();
        var resolution = registry.resolve("/open-insurance/customers/v2/personal/identifications");
        assertEquals(OpenInsurancePhase.FASE_2, resolution.orElseThrow().phase());
        assertEquals("customers.yaml", resolution.orElseThrow().sourceFile());
    }
}
