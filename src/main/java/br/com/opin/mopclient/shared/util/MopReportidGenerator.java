package br.com.opin.mopclient.shared.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Global utility for generating traceable mop-reportids.
 * Format: {@code mop-gateway-YYYYMMDD-HHmmss-SSS-xxxxxxxx}
 */
public final class MopReportidGenerator {

    private static final String SERVICE_PREFIX = "mop-gateway";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private MopReportidGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generate() {
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("%s-%s-%s", SERVICE_PREFIX, timestamp, uniqueId);
    }
}
