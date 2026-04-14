package br.com.opin.mopclient.anonymization.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the application mode.
 */
public enum ApplicationMode {
    TRANSMITTER,
    RECEIVER,
    INVALID_MODE;

    @JsonValue
    public String getValue() {
        return this.name();
    }

    public static String fromName(String name) {
        if (name == null || name.isBlank()) {
            return "ApplicationMode invalid: " + name + ". Valid values are: TRANSMITTER, RECEIVER, INVALID_MODE";
        }
        try {
            return ApplicationMode.valueOf(name.toUpperCase().trim()).getValue();
        } catch (IllegalArgumentException e) {
            return "ApplicationMode invalid: " + name + ". Valid values are: TRANSMITTER, RECEIVER, INVALID_MODE";
        }
    }
}
