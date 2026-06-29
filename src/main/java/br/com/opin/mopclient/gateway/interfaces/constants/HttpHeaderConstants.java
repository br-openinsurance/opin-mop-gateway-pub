package br.com.opin.mopclient.gateway.interfaces.constants;

public final class HttpHeaderConstants {

    /** Header name for operation: GET, POST, PUT, DELETE */
    public static final String OPERATION = "operation";

    /** Header name for httpType: Request or Response. */
    public static final String HTTP_TYPE = "httpType";

    /** Header name for HTTP status code (required when httpType is Response). */
    public static final String STATUS_CODE = "statusCode";

    /** Header name for origin. */
    public static final String ORIGIN = "origin";

    /** Header name for clientSSId (receiver). */
    public static final String CLIENT_SS_ID = "clientSSId";

    /** Header name for serverASId (transmitter). */
    public static final String SERVER_AS_ID = "serverASId";

    /** Header name for path. */
    public static final String PATH = "path";

    /** Header name for additional headers map. */
    public static final String HEADERS = "headers";

    public static final String RESPONSE = "response";

    public static final String MOP_REPORTID = "X-Mop-Reportid";

    /** Required header: correlation ID supplied by the client for tracing (X-Correlation-Id). */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    public static final String TIMESTAMP = "timestamp";

    /** Header name for traceOrigin (trace event origin, e.g. CLIENT). */
    public static final String TRACE_ORIGIN = "traceOrigin";

}
