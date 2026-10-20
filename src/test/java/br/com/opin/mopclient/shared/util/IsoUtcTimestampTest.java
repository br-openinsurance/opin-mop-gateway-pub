package br.com.opin.mopclient.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IsoUtcTimestamp")
class IsoUtcTimestampTest {

    @Test
    @DisplayName("formats without fraction of second and with Z")
    void formatsWithoutFraction() {
        Instant instant = Instant.parse("2026-10-15T17:44:33.014304846Z");
        assertEquals("2026-10-15T17:44:33Z", IsoUtcTimestamp.format(instant));
    }

    @Test
    @DisplayName("now matches yyyy-MM-dd'T'HH:mm:ss'Z'")
    void nowMatchesContract() {
        assertTrue(IsoUtcTimestamp.now().matches(IsoUtcTimestamp.REGEX));
    }

    @Test
    @DisplayName("strips nanos from Instant.toString values")
    void normalizesNanos() {
        assertEquals(
                "2026-10-15T17:44:33Z",
                IsoUtcTimestamp.normalizeOrNow("2026-10-15T17:44:33.014304846Z"));
    }
}
