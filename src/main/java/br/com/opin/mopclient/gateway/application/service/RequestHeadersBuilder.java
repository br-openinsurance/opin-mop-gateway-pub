package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.validator.application.service.MopPathResolver;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service for building RequestHeadersDTO objects from HTTP headers.
 * <p>
 * Encapsulates the logic for constructing RequestHeadersDTO with
 * all required and optional fields.
 */
@Service
public class RequestHeadersBuilder {

    private final TraceabilityService traceabilityService;
    private final MopPathResolver mopPathResolver;

    public RequestHeadersBuilder(TraceabilityService traceabilityService, MopPathResolver mopPathResolver) {
        this.traceabilityService = Objects.requireNonNull(traceabilityService, "TraceabilityService cannot be null");
        this.mopPathResolver = Objects.requireNonNull(mopPathResolver, "MopPathResolver cannot be null");
    }

    private static final String TRANSMITTER = "TRANSMITTER";
    private static final String RECEIVER = "RECEIVER";

    /**
     * Builds RequestHeadersDTO from individual header values and headers map.
     * correlationId is mandatory and supplied by the user (header X-Correlation-Id).
     * Header "origin" accepts only "client" or "server" (normalized to lowercase in the outbound payload).
     * Header "httpType" accepts "Request" or "Response" case-insensitively (normalized to lowercase in the outbound payload).
     *
     * @param correlationId  Correlation ID from header X-Correlation-Id (required, already validated)
     * @param origin         Origin header value ("client" or "server")
     * @param path           Path header value
     * @param operation      Operation header value
     * @param httpType         HTTP message type (required)
     * @param statusCode       HTTP status code (optional unless httpType is Response)
     * @param traceOrigin    Trace event origin, e.g. CLIENT (optional)
     * @param headers        Map of all request headers
     * @param clientSSId     Client SS identifier
     * @param serverASId     Server AS identifier
     * @return Built RequestHeadersDTO
     */
    public RequestHeadersDTO build(String correlationId, String origin, String path,
                                    String operation, String httpType, String statusCode,
                                    String traceOrigin,
                                    Map<String, String> headers,
                                    String clientSSId, String serverASId) {
        String correlationIdTrimmed = correlationId != null ? correlationId.trim() : null;
        traceabilityService.setCorrelationIdInContext(correlationIdTrimmed);
        String mopReportId = traceabilityService.getOrGenerateMopReportid(headers);
        String timestamp = traceabilityService.generateTimestamp();
        String resolvedClientSSId = (clientSSId != null && !clientSSId.isBlank())
                ? clientSSId
                : (headers != null ? headers.get("clientSSId") : null);
        String resolvedServerASId = (serverASId != null && !serverASId.isBlank())
                ? serverASId
                : (headers != null ? headers.get("serverASId") : null);
        String resolvedTraceOrigin = (traceOrigin != null && !traceOrigin.isBlank())
                ? traceOrigin
                : getHeaderIgnoreCase(headers, HttpHeaderConstants.TRACE_ORIGIN);
        String resolvedPath = path != null && !path.isBlank()
                ? mopPathResolver.resolveFromTransmitterUrl(path).mopPath()
                : path;
        String resolvedHttpType = normalizeHttpType((httpType != null && !httpType.isBlank())
                ? httpType.trim()
                : getHeaderIgnoreCase(headers, HttpHeaderConstants.HTTP_TYPE));
        String resolvedStatusCode = (statusCode != null && !statusCode.isBlank())
                ? statusCode.trim()
                : getHeaderIgnoreCase(headers, HttpHeaderConstants.STATUS_CODE);
        String normalizedOrigin = normalizeOrigin(origin);
        Map<String, String> resolvedHeaders = syncResolvedPathInHeaders(headers, resolvedPath);
        resolvedHeaders = syncOriginInHeaders(resolvedHeaders, normalizedOrigin);
        resolvedHeaders = syncHttpTypeInHeaders(resolvedHeaders, resolvedHttpType);

        return RequestHeadersDTO.builder()
                .correlationId(correlationIdTrimmed)
                .origin(normalizedOrigin)
                .path(resolvedPath)
                .operation(operation != null ? operation.trim() : null)
                .httpType(resolvedHttpType)
                .statusCode(resolvedStatusCode)
                .mopReportId(mopReportId)
                .timestamp(timestamp)
                .clientSSId(resolvedClientSSId)
                .serverASId(resolvedServerASId)
                .headers(resolvedHeaders)
                .traceOrigin(resolvedTraceOrigin)
                .build();
    }

    static String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return origin;
        }
        String trimmed = origin.trim();
        if ("client".equalsIgnoreCase(trimmed)) {
            return "client";
        }
        if ("server".equalsIgnoreCase(trimmed)) {
            return "server";
        }
        return trimmed;
    }

    static String normalizeHttpType(String httpType) {
        if (httpType == null || httpType.isBlank()) {
            return httpType;
        }
        String trimmed = httpType.trim();
        if ("request".equalsIgnoreCase(trimmed)) {
            return "request";
        }
        if ("response".equalsIgnoreCase(trimmed)) {
            return "response";
        }
        return trimmed;
    }

    private static Map<String, String> syncHttpTypeInHeaders(Map<String, String> headers, String normalizedHttpType) {
        if (normalizedHttpType == null || normalizedHttpType.isBlank()) {
            return headers;
        }
        Map<String, String> copy = headers != null ? new HashMap<>(headers) : new HashMap<>();
        copy.put(HttpHeaderConstants.HTTP_TYPE, normalizedHttpType);
        return copy;
    }

    private static Map<String, String> syncOriginInHeaders(Map<String, String> headers, String normalizedOrigin) {
        if (normalizedOrigin == null || normalizedOrigin.isBlank()) {
            return headers;
        }
        Map<String, String> copy = headers != null ? new HashMap<>(headers) : new HashMap<>();
        copy.put(HttpHeaderConstants.ORIGIN, normalizedOrigin);
        return copy;
    }

    private static Map<String, String> syncResolvedPathInHeaders(Map<String, String> headers, String resolvedPath) {
        if (resolvedPath == null || resolvedPath.isBlank()) {
            return headers;
        }
        Map<String, String> copy = headers != null ? new HashMap<>(headers) : new HashMap<>();
        copy.put(HttpHeaderConstants.PATH, resolvedPath);
        return copy;
    }

    private static String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) return null;
        if (headers.containsKey(name)) return headers.get(name);
        return headers.entrySet().stream()
                .filter(e -> name.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}