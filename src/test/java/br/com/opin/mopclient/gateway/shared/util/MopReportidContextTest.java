package br.com.opin.mopclient.shared.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MopReportidContext Tests")
class MopReportidContextTest {

    @BeforeEach
    void setUp() {
        MopReportidContext.clear();
    }

    @AfterEach
    void tearDown() {
        MopReportidContext.clear();
    }

    @Test
    @DisplayName("Sets mop-reportid when provided")
    void shouldSetMopReportidWhenProvided() {
        // Arrange
        String mopReportid = "test-mop-report-id-123";

        // Act
        MopReportidContext.setMopReportid(mopReportid);

        // Assert
        assertEquals(mopReportid, MopReportidContext.getMopReportid());
        assertEquals(mopReportid, MDC.get("mopReportid"));
    }

    @Test
    @DisplayName("Generates mop-reportid when given null, empty, or blank")
    void shouldGenerateMopReportidWhenProvidedNullOrEmptyOrBlank() {
        for (String invalid : new String[]{null, "", "   "}) {
            MopReportidContext.setMopReportid(invalid);
            String mopReportid = MopReportidContext.getMopReportid();
            assertNotNull(mopReportid);
            assertTrue(mopReportid.startsWith("mop-gateway-"));
            MopReportidContext.clear();
        }
    }


    @Test
    @DisplayName("Clears mop-reportid from MDC")
    void shouldClearMopReportidFromMDC() {
        // Arrange
        MopReportidContext.setMopReportid("test-id");

        // Act
        MopReportidContext.clear();

        // Assert
        assertNull(MopReportidContext.getMopReportid());
        assertFalse(MopReportidContext.isSet());
    }

    @Test
    @DisplayName("Returns true when mop-reportid is set")
    void shouldReturnTrueWhenMopReportidIsSet() {
        // Arrange
        MopReportidContext.setMopReportid("test-id");

        // Act & Assert
        assertTrue(MopReportidContext.isSet());
    }

    @Test
    @DisplayName("Overwrites existing mop-reportid")
    void shouldOverwriteExistingMopReportid() {
        // Arrange
        MopReportidContext.setMopReportid("old-id");

        // Act
        MopReportidContext.setMopReportid("new-id");

        // Assert
        assertEquals("new-id", MopReportidContext.getMopReportid());
    }
}
