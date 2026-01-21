package br.com.opin.mopclient.gateway.interfaces.enums;

/**
 * Enum representing valid HTTP methods for the operation header.
 * <p>
 * This enum defines the allowed values for the {@code operation} header
 * in anonymization requests. Only these values are accepted:
 * <ul>
 *   <li>GET - Retrieve data</li>
 *   <li>POST - Create or submit data</li>
 *   <li>PUT - Update/replace data</li>
 *   <li>PATCH - Partial update</li>
 *   <li>DELETE - Delete data</li>
 *   <li>HEAD - Retrieve headers only</li>
 *   <li>OPTIONS - Get allowed methods</li>
 *   <li>TRACE - Echo request for debugging</li>
 * </ul>
 * <p>
 * <strong>Usage:</strong>
 * This enum is used by {@link br.com.opin.mopclient.gateway.interfaces.validation.HeaderValidator}
 * to validate the operation header value in incoming requests.
 *
 * @author MOP Team
 * @since 1.0
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;

    /**
     * Checks if a given string value matches any of the enum values (case-insensitive).
     *
     * @param value the string value to check
     * @return true if the value matches any enum value (case-insensitive), false otherwise
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets the HttpMethod enum value from a string (case-insensitive).
     *
     * @param value the string value to convert
     * @return the HttpMethod enum value, or null if not found
     */
    public static HttpMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets a comma-separated string of all valid HTTP method names.
     *
     * @return a string with all valid method names separated by commas
     */
    public static String getValidValues() {
        StringBuilder sb = new StringBuilder();
        HttpMethod[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values[i].name());
        }
        return sb.toString();
    }
}

