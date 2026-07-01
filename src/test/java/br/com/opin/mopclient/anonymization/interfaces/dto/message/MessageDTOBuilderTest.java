package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("MessageDTOBuilder Tests")
class MessageDTOBuilderTest {

    private static final String CORRELATION_ID = "corr-123";
    private static final String TRACE_ORIGIN = "CLIENT";

    @Test
    @DisplayName("Includes traceOrigin in TraceDTO when header is provided")
    void shouldIncludeTraceOriginInTraceWhenProvided() {
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
                .origin("client")
                .path("/path")
                .operation("POST")
                .clientSSId("RECEIVER")
                .serverASId("TRANSMITTER")
                .traceOrigin(TRACE_ORIGIN)
                .build();

        MessageDTO message = MessageDTOBuilder.buildFromHeaders(
                headers,
                Collections.emptySet(),
                Collections.emptySet(),
                List.of(),
                "{}",
                "localhost",
                "http://localhost/path",
                "org-id");

        assertEquals(TRACE_ORIGIN, message.getTrace().getTraceOrigin());
    }

    @Test
    @DisplayName("Uses empty traceOrigin in TraceDTO when header is absent")
    void shouldUseEmptyTraceOriginWhenAbsent() {
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
                .origin("client")
                .path("/path")
                .operation("POST")
                .clientSSId("RECEIVER")
                .serverASId("TRANSMITTER")
                .build();

        MessageDTO message = MessageDTOBuilder.buildFromHeaders(
                headers,
                Collections.emptySet(),
                Collections.emptySet(),
                List.of(),
                "{}",
                "localhost",
                "http://localhost/path",
                "org-id");

        assertEquals("", message.getTrace().getTraceOrigin());
    }

    @Test
    @DisplayName("Includes inbound HTTP headers in request.header")
    void shouldIncludeInboundHeadersInRequestHeader() {
        Map<String, String> inboundHeaders = Map.of(
                "X-Correlation-Id", CORRELATION_ID,
                "origin", "client",
                "path", "/path",
                "operation", "POST",
                "Content-Type", "application/json");
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
                .origin("client")
                .path("/path")
                .operation("POST")
                .headers(inboundHeaders)
                .build();

        MessageDTO message = MessageDTOBuilder.buildFromHeaders(
                headers,
                Collections.emptySet(),
                Collections.emptySet(),
                List.of(),
                "{}",
                "localhost",
                "http://localhost/path",
                "org-id");

        assertNotNull(message.getRequest().getHeader());
        assertEquals("application/json", message.getRequest().getHeader().get("Content-Type"));
        assertEquals(CORRELATION_ID, message.getRequest().getHeader().get("X-Correlation-Id"));
    }

    @Test
    @DisplayName("Includes mopReportid in trace when provided")
    void shouldIncludeMopReportidInTrace() {
        String mopReportid = "550e8400-e29b-41d4-a716-446655440001";
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
                .mopReportid(mopReportid)
                .origin("client")
                .path("/path")
                .operation("POST")
                .build();

        MessageDTO message = MessageDTOBuilder.buildFromHeaders(
                headers,
                Collections.emptySet(),
                Collections.emptySet(),
                List.of(),
                "{}",
                "localhost",
                "http://localhost/path",
                "org-id");

        assertEquals(mopReportid, message.getTrace().getMopReportid());
    }
}
