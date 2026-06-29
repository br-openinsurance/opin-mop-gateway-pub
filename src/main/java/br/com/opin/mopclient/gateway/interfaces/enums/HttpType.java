package br.com.opin.mopclient.gateway.interfaces.enums;

/**
 * Allowed values for the {@code httpType} header: {@code Request} or {@code Response}.
 */
public enum HttpType {
    REQUEST,
    RESPONSE;

    public static boolean isValid(String value) {
        return fromString(value) != null;
    }

    public static HttpType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (HttpType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isResponse(String value) {
        return RESPONSE == fromString(value);
    }

    public static String getValidValues() {
        return "Request, Response";
    }
}
