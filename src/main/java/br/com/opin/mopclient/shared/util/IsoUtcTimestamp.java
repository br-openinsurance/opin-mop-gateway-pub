package br.com.opin.mopclient.shared.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Timestamp UTC dos envios do MOP Client (payload de rastreio):
 * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} — sem fração de segundo.
 */
public final class IsoUtcTimestamp {

    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String REGEX = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN).withZone(ZoneOffset.UTC);

    private IsoUtcTimestamp() {
    }

    public static String now() {
        return format(Instant.now());
    }

    public static String format(Instant instant) {
        return FORMATTER.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Converte ISO-8601 (com ou sem fração) para segundos + {@code Z}.
     * Valor vazio ou inválido gera o instante atual.
     */
    public static String normalizeOrNow(String value) {
        if (value == null || value.isBlank()) {
            return now();
        }
        if (value.matches(REGEX)) {
            return value;
        }
        try {
            return format(Instant.parse(value));
        } catch (DateTimeParseException ex) {
            return now();
        }
    }
}
