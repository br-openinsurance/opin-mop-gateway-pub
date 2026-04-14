package br.com.opin.mopclient.anonymization.shared.util;

import br.com.opin.mopclient.anonymization.shared.constant.MopReportid;
import org.slf4j.MDC;

/**
 * Manager for mop-reportid operations throughout the application.
 */
public final class MopReportidManager {

    private MopReportidManager() {
        // Utility class - prevent instantiation
    }

    public static void setMopReportid(String mopReportid) {
        if (mopReportid != null && !mopReportid.isBlank()) {
            MDC.put(MopReportid.MDC_KEY, mopReportid);
        }
    }

    public static String getMopReportid() {
        return MDC.get(MopReportid.MDC_KEY);
    }

    public static void clearMopReportid() {
        MDC.remove(MopReportid.MDC_KEY);
    }

    /**
     * Extracts mop-reportid from headers map (used in unified architecture).
     */
    public static String extractFromHeaders(java.util.Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        String mopReportid = headers.get(MopReportid.HEADER_NAME);
        return mopReportid != null && !mopReportid.isBlank() ? mopReportid : null;
    }
}
