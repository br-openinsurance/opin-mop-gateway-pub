package br.com.opin.mopclient.validator.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenApiPathMatcher")
class OpenApiPathMatcherTest {

    @Test
    @DisplayName("extractBasePath from server URL")
    void extractBasePathFromServerUrl() {
        assertEquals(
                "/open-insurance/consents/v3",
                OpenApiPathMatcher.extractBasePath("https://api.seguro.com.br/open-insurance/consents/v3"));
    }

    @Test
    @DisplayName("toRelativePath after basePath")
    void toRelativePathAfterBase() {
        assertEquals(
                "/consents",
                OpenApiPathMatcher.toRelativePath(
                        "/open-insurance/consents/v3/consents",
                        "/open-insurance/consents/v3"));
    }

    @Test
    @DisplayName("pathTemplateMatches consentId parameter")
    void pathTemplateMatchesConsentId() {
        assertTrue(OpenApiPathMatcher.pathTemplateMatches(
                "/consents/{consentId}",
                "/consents/urn:prudential:C1DD93123"));
    }

    @Test
    @DisplayName("pathTemplateMatches business open data path")
    void pathTemplateMatchesBusiness() {
        assertTrue(OpenApiPathMatcher.pathTemplateMatches(
                "/business",
                "/business"));
    }

    @Test
    @DisplayName("extractOpenInsurancePath from full URL")
    void extractOpenInsurancePathFromFullUrl() {
        assertEquals(
                "/open-insurance/customers/v2/personal/identifications",
                OpenApiPathMatcher.extractOpenInsurancePath(
                        "https://api.seguro.com.br/open-insurance/customers/v2/personal/identifications"));
    }

    @Test
    @DisplayName("extractOpenInsurancePath from path already starting with open-insurance")
    void extractOpenInsurancePathFromRelativePath() {
        assertEquals(
                "/open-insurance/customers/v2/personal/identifications",
                OpenApiPathMatcher.extractOpenInsurancePath(
                        "/open-insurance/customers/v2/personal/identifications"));
    }

    @Test
    @DisplayName("extractOpenInsurancePath ignores prefix before open-insurance")
    void extractOpenInsurancePathSkipsPrefix() {
        assertEquals(
                "/open-insurance/consents/v3/consents",
                OpenApiPathMatcher.extractOpenInsurancePath(
                        "/gateway/proxy/open-insurance/consents/v3/consents"));
    }

    @Test
    @DisplayName("extractOpenInsurancePath falls back when open-insurance is absent")
    void extractOpenInsurancePathFallbackWithoutPrefix() {
        assertEquals(
                "/report-api/v1/server-event",
                OpenApiPathMatcher.extractOpenInsurancePath(
                        "https://ingestion-api.opinbrasil.com.br/report-api/v1/server-event"));
    }

    @Test
    @DisplayName("extractOpenInsurancePath from path prefixed with HTTP method and full URL")
    void extractOpenInsurancePathFromMethodPrefixedUrl() {
        assertEquals(
                "/open-insurance/consents/v3/consents",
                OpenApiPathMatcher.extractOpenInsurancePath(
                        "POST https://api.seguro.com.br/open-insurance/consents/v3/consents"));
    }

    @Test
    @DisplayName("extractLeadingHttpMethod from method-prefixed path")
    void extractLeadingHttpMethodFromPrefixedPath() {
        assertEquals("POST", OpenApiPathMatcher.extractLeadingHttpMethod(
                "POST https://api.seguro.com.br/open-insurance/consents/v3/consents").orElseThrow());
        assertTrue(OpenApiPathMatcher.extractLeadingHttpMethod(
                "/open-insurance/consents/v3/consents").isEmpty());
    }

    @Test
    @DisplayName("substitutePathParameters replaces placeholders")
    void substitutePathParametersReplacesPlaceholders() {
        assertEquals(
                "/insurance-auto/abc-123/policy-info",
                OpenApiPathMatcher.substitutePathParameters(
                        "/insurance-auto/{policyId}/policy-info",
                        java.util.Map.of("policyId", "abc-123")));
    }

    @Test
    @DisplayName("buildMopPath concatenates basePath and template with params")
    void buildMopPathFromTemplate() {
        assertEquals(
                "/open-insurance/insurance-auto/v1/insurance-auto/abc-123/policy-info",
                OpenApiPathMatcher.buildMopPath(
                        "/open-insurance/insurance-auto/v1",
                        "/insurance-auto/{policyId}/policy-info",
                        java.util.Map.of("policyId", "abc-123")));
    }
}
