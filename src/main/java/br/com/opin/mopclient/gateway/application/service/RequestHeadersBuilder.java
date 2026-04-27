package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import org.springframework.stereotype.Service;

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

    public RequestHeadersBuilder(TraceabilityService traceabilityService) {
        this.traceabilityService = Objects.requireNonNull(traceabilityService, "TraceabilityService cannot be null");
    }

    private static final String TRANSMITTER = "TRANSMITTER";
    private static final String RECEIVER = "RECEIVER";

    /**
     * Builds RequestHeadersDTO from individual header values and headers map.
     * correlationId is mandatory and supplied by the user (header X-Correlation-Id).
     * Header "origin" accepts only "client" or "server".
     *
     * @param correlationId  Correlation ID from header X-Correlation-Id (required, already validated)
     * @param origin         Origin header value ("client" or "server")
     * @param path           Path header value
     * @param operation      Operation header value
     * @param step           Step of the flow in the trace (required)
     * @param dataEventoStep Timestamp of the step event, ISO-8601 (required)
     * @param headers        Map of all request headers (traceOrigin optional)
     * @param clientSSId     Client SS identifier
     * @param serverASId     Server AS identifier
     * @return Built RequestHeadersDTO
     */
    public RequestHeadersDTO build(String correlationId, String origin, String path,
                                    String operation,
                                    String step, String dataEventoStep,
                                    Map<String, String> headers,
                                    String clientSSId, String serverASId) {
        String correlationIdTrimmed = correlationId != null ? correlationId.trim() : null;
        traceabilityService.setCorrelationIdInContext(correlationIdTrimmed);
        String mopReportid = traceabilityService.getOrGenerateMopReportid(headers);
        String timestamp = traceabilityService.generateTimestamp();
        String resolvedClientSSId = (clientSSId != null && !clientSSId.isBlank())
                ? clientSSId
                : (headers != null ? headers.get("clientSSId") : null);
        String resolvedServerASId = (serverASId != null && !serverASId.isBlank())
                ? serverASId
                : (headers != null ? headers.get("serverASId") : null);
        String traceOrigin = getHeaderIgnoreCase(headers, HttpHeaderConstants.TRACE_ORIGIN);

        return RequestHeadersDTO.builder()
                .correlationId(correlationIdTrimmed)
                .origin(origin)
                .path(path)
                .operation(operation)
                .mopReportid(mopReportid)
                .timestamp(timestamp)
                .clientSSId(resolvedClientSSId)
                .serverASId(resolvedServerASId)
                .headers(headers)
                .step(step)
                .dataEventoStep(dataEventoStep)
                .traceOrigin(traceOrigin)
                .build();
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