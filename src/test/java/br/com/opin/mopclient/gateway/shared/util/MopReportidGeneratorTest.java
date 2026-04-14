package br.com.opin.mopclient.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MopReportidGenerator Tests")
class MopReportidGeneratorTest {

    private static final String SERVICE_PREFIX = "mop-gateway";

    @Test
    @DisplayName("Generates MOP Report ID with expected format")
    void shouldGenerateMopReportidWithExpectedFormat() {
        String mopReportid = MopReportidGenerator.generate();

        assertNotNull(mopReportid);
        assertTrue(mopReportid.startsWith(SERVICE_PREFIX + "-"));
        String[] parts = mopReportid.split("-");
        assertTrue(parts.length >= 4);
    }

    @Test
    @DisplayName("Generates unique MOP Report IDs")
    void shouldGenerateUniqueMopReportids() {
        String mopReportid1 = MopReportidGenerator.generate();
        String mopReportid2 = MopReportidGenerator.generate();

        assertNotEquals(mopReportid1, mopReportid2);
    }

    @Test
    @DisplayName("Generates MOP Report ID with valid timestamp")
    void shouldGenerateMopReportidWithValidTimestamp() {
        String mopReportid = MopReportidGenerator.generate();

        assertTrue(mopReportid.matches("mop-gateway-\\d{8}-\\d{6}-\\d{3}-[a-fA-F0-9]{8}"));
    }
}
