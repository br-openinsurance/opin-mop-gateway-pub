package br.com.opin.mopclient.validator.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenApiSpecFileVersion")
class OpenApiSpecFileVersionTest {

    @Test
    @DisplayName("rewrites companion file basePath to the filename version")
    void rewritesCompanionBasePath() {
        assertEquals(
                "/open-insurance/customers/v1",
                OpenApiSpecFileVersion.applyToBasePath(
                        "customers_v1.yaml",
                        "/open-insurance/customers/v2"));
    }

    @Test
    @DisplayName("maps companion file to canonical phase")
    void companionInheritsCanonicalPhase() {
        assertEquals(OpenInsurancePhase.FASE_2, OpenApiSpecPhaseCatalog.phaseForFile("customers_v1.yaml"));
        assertEquals(OpenInsurancePhase.FASE_2_AND_3, OpenApiSpecPhaseCatalog.phaseForFile("consents_v1.yaml"));
    }

    @Test
    @DisplayName("extracts canonical file name from companion")
    void extractsCanonicalFileName() {
        assertEquals("customers.yaml", OpenApiSpecFileVersion.canonicalFileName("customers_v1.yaml").orElseThrow());
        assertTrue(OpenApiSpecFileVersion.canonicalFileName("customers.yaml").isEmpty());
    }
}
