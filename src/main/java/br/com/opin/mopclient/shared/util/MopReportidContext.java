package br.com.opin.mopclient.shared.util;

import org.slf4j.MDC;

/**
 * Global utility for managing mop-reportid in MDC.
 */
public final class MopReportidContext {

    private static final String MDC_KEY = "mopReportid";

    private MopReportidContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void setMopReportid(String mopReportid) {
        if (mopReportid == null || mopReportid.isBlank()) {
            mopReportid = MopReportidGenerator.generate();
        }
        MDC.put(MDC_KEY, mopReportid);
    }

    public static String getMopReportid() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    public static boolean isSet() {
        return MDC.get(MDC_KEY) != null;
    }
}
