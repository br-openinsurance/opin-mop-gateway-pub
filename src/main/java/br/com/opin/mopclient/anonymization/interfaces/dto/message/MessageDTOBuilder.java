package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MessageDTOBuilder {

    private MessageDTOBuilder() {
    }

    public static MessageDTO buildFromHeaders(
            RequestHeadersDTO headersDTO,
            Set<String> anonymizedFields,
            Set<String> exposedFields,
            List<Validation> validations,
            String payloadData,
            String host,
            String url,
            String step,
            String orgId) {

        String correlationId = headersDTO.getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("TraceDTO requires correlationId (header X-Correlation-Id); it is mandatory.");
        }
        String mopReportid = headersDTO.getMopReportid();
        String origin = headersDTO.getOrigin();
        String path = headersDTO.getPath();
        String operation = headersDTO.getOperation();
        String clientSSId = headersDTO.getClientSSId() != null && !headersDTO.getClientSSId().isBlank()
                ? headersDTO.getClientSSId() : origin;
        String serverASId = headersDTO.getServerASId() != null && !headersDTO.getServerASId().isBlank()
                ? headersDTO.getServerASId() : "";
        String timestamp = headersDTO.getTimestamp();

        if (timestamp == null || timestamp.isBlank()) {
            timestamp = Instant.now().toString();
        }

        Map<String, String> headers = headersDTO.getHeaders() != null ? headersDTO.getHeaders() : Map.of();
        String method = extractMethod(headers);

        String traceStep = step != null && !step.isBlank() ? step : "request-received";
        String traceDataEventoStep = Instant.now().toString();
        String traceOrigin = headersDTO.getTraceOrigin() != null && !headersDTO.getTraceOrigin().isBlank()
                ? headersDTO.getTraceOrigin()
                : "";
        String httpType = headersDTO.getHttpType() != null ? headersDTO.getHttpType() : "";
        String statusCode = headersDTO.getStatusCode() != null ? headersDTO.getStatusCode() : "";

        MetadataDTO metadata = MetadataDTO.builder()
                .version("1.0")
                .environment("sandbox")
                .module("MOP")
                .initiatedBy("mop-client-gateway")
                .purpose("Data anonymization and routing")
                .timestamp(timestamp)
                .contentType("application/json")
                .build();

        TraceDTO trace = TraceDTO.builder()
                .correlationId(correlationId)
                .mopReportid(mopReportid != null ? mopReportid : correlationId)
                .traceId(generateTraceId())
                .path(path != null ? path : "")
                .operation(operation != null ? operation : "")
                .httpType(httpType)
                .statusCode(statusCode)
                .clientSSId(clientSSId != null ? clientSSId : "")
                .serverASId(serverASId != null ? serverASId : "")
                .step(traceStep)
                .dataEventoStep(traceDataEventoStep)
                .origin(origin != null ? origin : "")
                .traceOrigin(traceOrigin)
                .orgId(orgId != null ? orgId : "")
                .build();

        HttpRequestInfoDTO request = HttpRequestInfoDTO.builder()
                .method(method != null ? method : "POST")
                .host(host)
                .url(url)
                .build();

        PrivacyDTO privacy = PrivacyDTO.builder()
                .anonymizedFields(anonymizedFields != null ? List.copyOf(anonymizedFields) : List.of())
                .exposedFields(exposedFields != null ? List.copyOf(exposedFields) : List.of())
                .build();

        PayloadDTO payload = PayloadDTO.builder()
                .data(payloadData)
                .build();

        return MessageDTO.builder()
                .metadata(metadata)
                .trace(trace)
                .request(request)
                .privacy(privacy)
                .validations(validations != null ? validations : List.of())
                .payload(payload)
                .build();
    }

    private static String extractMethod(Map<String, String> headers) {
        return headers.getOrDefault("method", 
                headers.getOrDefault("Method", 
                        headers.getOrDefault("X-HTTP-Method-Override", "POST")));
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
