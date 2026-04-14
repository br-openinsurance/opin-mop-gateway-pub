package br.com.opin.mopclient.anonymization.shared.constant;

/**
 * Constants for mop-reportid (request tracking) management.
 */
public final class MopReportid {

    public static final String MDC_KEY = "mopReportid";
    public static final String HEADER_NAME = "X-Mop-Reportid";

    private MopReportid() {
        // Utility class - prevent instantiation
    }
}
