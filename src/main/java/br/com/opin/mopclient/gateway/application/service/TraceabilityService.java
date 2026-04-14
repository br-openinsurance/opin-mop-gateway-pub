package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.shared.util.MopReportidContext;
import br.com.opin.mopclient.shared.util.MopReportidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.MOP_REPORTID;

/**
 * Service for managing traceability information (mop-reportid and timestamp).
 * <p>
 * Handles generation and management of mop-reportids and timestamps
 * for request tracking and audit purposes.
 */
@Service
public class TraceabilityService {

    /**
     * Sets the correlation ID in MDC context (from mandatory header X-Correlation-Id).
     *
     * @param correlationId Correlation ID informed by the user (must not be null or empty)
     */
    public void setCorrelationIdInContext(String correlationId) {
        if (StringUtils.hasText(correlationId)) {
            MopReportidContext.setMopReportid(correlationId.trim());
        }
    }

    /**
     * Generates or retrieves mop-reportid from headers.
     * If not present in headers, generates a new traceable mop-reportid.
     * Also sets the mop-reportid in MDC context.
     *
     * @param headers Map of request headers
     * @return MOP report ID (generated or from headers)
     */
    public String getOrGenerateMopReportid(Map<String, String> headers) {
        String mopReportid = headers != null ? headers.get(MOP_REPORTID) : null;
        if (!StringUtils.hasText(mopReportid)) {
            mopReportid = MopReportidGenerator.generate();
        }
        MopReportidContext.setMopReportid(mopReportid);
        return mopReportid;
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

