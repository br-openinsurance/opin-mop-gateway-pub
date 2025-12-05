package br.com.opin.mopclient.gateway.shared.util;

import org.slf4j.MDC;

/**
 * Utility class for managing correlation ID in the Mapped Diagnostic Context (MDC).
 * <p>
 * This class provides thread-safe methods to set, get, and clear correlation IDs
 * in the MDC, enabling automatic inclusion of correlation IDs in all log statements
 * throughout the application execution flow.
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 * // Set correlation ID from HTTP header
 * CorrelationIdContext.setCorrelationId(requestHeader);
 * 
 * // Get current correlation ID
 * String correlationId = CorrelationIdContext.getCorrelationId();
 * 
 * // Clear correlation ID at end of request
 * CorrelationIdContext.clear();
 * }</pre>
 * <p>
 * <strong>MDC Key:</strong> The correlation ID is stored in MDC with the key {@code correlationId}.
 * Configure your logging pattern to include {@code %X{correlationId}} to automatically
 * include it in all log entries.
 *
 * @author MOP Team
 * @since 1.0
 */
public final class CorrelationIdContext {

    private static final String MDC_KEY = "correlationId";

    private CorrelationIdContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sets the correlation ID in the MDC.
     * <p>
     * If the provided correlationId is null or blank, generates a new traceable correlation ID.
     *
     * @param correlationId the correlation ID to set (can be null or blank)
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = MessageBuilderHelper.generateTraceableCorrelationId();
        }
        MDC.put(MDC_KEY, correlationId);
    }

    /**
     * Gets the current correlation ID from the MDC.
     *
     * @return the correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(MDC_KEY);
    }

    /**
     * Clears the correlation ID from the MDC.
     * <p>
     * Should be called at the end of request processing to prevent correlation ID
     * leakage between requests in thread pools.
     */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    /**
     * Checks if a correlation ID is currently set in the MDC.
     *
     * @return true if correlation ID is set, false otherwise
     */
    public static boolean isSet() {
        return MDC.get(MDC_KEY) != null;
    }
}

