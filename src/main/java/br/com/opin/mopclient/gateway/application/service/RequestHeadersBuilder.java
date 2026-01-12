package br.com.opin.mopclient.gateway.application.service;

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

    /**
     * Builds RequestHeadersDTO from individual header values and headers map.
     *
     * @param origin          Origin header value
     * @param destination     Destination header value
     * @param path            Path header value
     * @param operation       Operation header value
     * @param userID          UserID header value
     * @param applicationMode ApplicationMode header value
     * @param headers         Map of all request headers
     * @return Built RequestHeadersDTO
     */
    public RequestHeadersDTO build(String origin, String destination, String path,
                                    String operation, String userID,
                                    String applicationMode, Map<String, String> headers) {
        String correlationId = traceabilityService.getOrGenerateCorrelationId(headers);
        String timestamp = traceabilityService.generateTimestamp();

        return RequestHeadersDTO.builder()
                .origin(origin)
                .destination(destination)
                .path(path)
                .operation(operation)
                .userID(userID)
                .applicationMode(applicationMode)
                .correlationID(correlationId)
                .timestamp(timestamp)
                .headers(headers)
                .build();
    }
}

