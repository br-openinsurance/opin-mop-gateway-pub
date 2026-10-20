package br.com.opin.mopclient.consentfunnel.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formato de data/hora do contrato do funil: {@code yyyy-MM-dd'T'HH:mm:ss}
 * (sem fração de segundo e sem {@code Z}).
 */
public final class ConsentFunnelDateTime {

    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String REGEX = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$";
    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN).withZone(ZoneOffset.UTC);

    private ConsentFunnelDateTime() {
    }

    public static String formatNow() {
        return format(Instant.now(Clock.systemUTC()));
    }

    public static String format(Instant instant) {
        return FORMATTER.format(instant);
    }

    public static boolean isValid(String value) {
        return value != null && value.matches(REGEX);
    }
}
