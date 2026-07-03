package br.com.opin.mopclient.validator.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenApiSpecCompatibilityPatcher false positives")
class OpenApiSpecCompatibilityPatcherFalsePositiveTest {

    @Test
    @DisplayName("detects openapi4j 1007 false positive for string shareholding")
    void detectsShareholdingFormatDoubleFalsePositive() {
        assertTrue(OpenApiSpecCompatibilityPatcher.isStringNumericFormatFalsePositive(
                1007, "Value '0.51' does not match format 'double'."));
        assertTrue(OpenApiSpecCompatibilityPatcher.isStringNumericFormatFalsePositive(
                1007, "Value '0.510000' does not match format 'double'."));
        assertFalse(OpenApiSpecCompatibilityPatcher.isStringNumericFormatFalsePositive(
                1025, "'0.51' does not respect pattern '^0\\.\\d+$'."));
    }
}
