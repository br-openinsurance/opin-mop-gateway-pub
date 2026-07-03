package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessageDTOBuilder Tests")
class MessageDTOBuilderTest {

    private static final String CORRELATION_ID = "corr-123";
    private static final String TRACE_ORIGIN = "CLIENT";
    private static final String METADATA_VERSION = "1.0.5";
    private static final String METADATA_ENVIRONMENT = "sandbox";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

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
                "org-id",
                METADATA_VERSION,
                METADATA_ENVIRONMENT);

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
                "org-id",
                METADATA_VERSION,
                METADATA_ENVIRONMENT);

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
                "org-id",
                METADATA_VERSION,
                METADATA_ENVIRONMENT);

        assertNotNull(message.getRequest().getHeader());
        assertEquals("application/json", message.getRequest().getHeader().get("Content-Type"));
        assertEquals(CORRELATION_ID, message.getRequest().getHeader().get("X-Correlation-Id"));
    }

    @Test
    @DisplayName("Includes mopReportId in trace when provided")
    void shouldIncludeMopReportIdInTrace() {
        String mopReportId = "550e8400-e29b-41d4-a716-446655440001";
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
                .mopReportId(mopReportId)
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
                "org-id",
                METADATA_VERSION,
                METADATA_ENVIRONMENT);

        assertEquals(mopReportId, message.getTrace().getMopReportId());
    }

    @Test
    @DisplayName("Includes pom artifact version in metadata.version")
    void shouldIncludeMetadataVersionFromPom() {
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
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
                "org-id",
                METADATA_VERSION,
                METADATA_ENVIRONMENT);

        assertEquals(METADATA_VERSION, message.getMetadata().getVersion());
        assertEquals(METADATA_ENVIRONMENT, message.getMetadata().getEnvironment());
    }

    @Test
    @DisplayName("Serializes validations summary inside request in outbound MessageDTO")
    void shouldSerializeValidationsSummaryInsideRequest() throws Exception {
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId(CORRELATION_ID)
                .origin("client")
                .path("/path")
                .operation("POST")
                .build();

        List<Validation> validations = List.of(
                Validation.builder()
                        .violation("Field 'data' is required.")
                        .code("1026")
                        .severity("ERROR")
                        .attribute("body")
                        .build(),
                Validation.builder()
                        .violation("Field 'links' is required.")
                        .code("1026")
                        .severity("ERROR")
                        .attribute("body")
                        .build(),
                Validation.builder()
                        .violation("Field 'meta' is required.")
                        .code("1026")
                        .severity("ERROR")
                        .attribute("body")
                        .build());

        MessageDTO message = MessageDTOBuilder.buildFromHeaders(
                headers,
                Collections.emptySet(),
                Collections.emptySet(),
                validations,
                "{}",
                "localhost",
                "http://localhost/path",
                "org-id",
                METADATA_VERSION,
                METADATA_ENVIRONMENT);

        assertNotNull(message.getRequest().getValidations());
        assertEquals("ERROR", message.getRequest().getValidations().getStatus());
        assertEquals(3, message.getRequest().getValidations().getTotal());
        assertEquals(3, message.getRequest().getValidations().getPending().size());

        String json = OBJECT_MAPPER.writeValueAsString(message);
        assertNotNull(json);
        assertTrue(json.startsWith("{\"metadata\""));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"validations\""));
        int requestIndex = json.indexOf("\"request\"");
        int validationsIndex = json.indexOf("\"validations\"", requestIndex);
        int payloadIndex = json.indexOf("\"payload\"");
        assertTrue(validationsIndex > requestIndex);
        assertTrue(payloadIndex > validationsIndex);
        assertTrue(json.contains("\"status\":\"ERROR\""));
        assertTrue(json.contains("\"total\":3"));
        assertTrue(json.contains("Field 'data' is required."));
    }
}
