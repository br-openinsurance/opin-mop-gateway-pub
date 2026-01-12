package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import br.com.opin.mopclient.gateway.shared.util.MessageBuilderHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;

/**
 * Service for managing traceability information (correlation ID and timestamp).
 * <p>
 * Handles generation and management of correlation IDs and timestamps
 * for request tracking and audit purposes.
 */
@Service
public class TraceabilityService {

    /**
     * Generates or retrieves correlation ID from headers.
     * If not present in headers, generates a new traceable correlation ID.
     * Also sets the correlation ID in MDC context.
     *
     * @param headers Map of request headers
     * @return Correlation ID (generated or from headers)
     */
    public String getOrGenerateCorrelationId(Map<String, String> headers) {
        String correlationId = headers != null ? headers.get(CORRELATIONID) : null;
        if (!StringUtils.hasText(correlationId)) {
            correlationId = MessageBuilderHelper.generateTraceableCorrelationId();
        }
        CorrelationIdContext.setCorrelationId(correlationId);
        return correlationId;
    }

    /**
     * Generates current timestamp in ISO-8601 format.
     *
     * @return Timestamp string
     */
    public String generateTimestamp() {
        return Instant.now().toString();
    }
}

