package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                null,
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
                null,
                "org-id");

        assertEquals("", message.getTrace().getTraceOrigin());
    }
}
