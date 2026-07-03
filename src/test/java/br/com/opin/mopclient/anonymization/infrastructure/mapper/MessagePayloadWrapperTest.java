package br.com.opin.mopclient.anonymization.infrastructure.mapper;

import br.com.opin.mopclient.anonymization.interfaces.dto.message.MessageDTO;
import br.com.opin.mopclient.anonymization.interfaces.dto.message.MessageDTOBuilder;
import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessagePayloadWrapper")
class MessagePayloadWrapperTest {

    private MessagePayloadWrapper wrapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        wrapper = new MessagePayloadWrapper(objectMapper);
    }

    @Test
    @DisplayName("serializes metadata first and keeps root field order")
    void serializesMetadataFirst() throws Exception {
        RequestHeadersDTO headers = RequestHeadersDTO.builder()
                .correlationId("corr-123")
                .origin("client")
                .path("/open-insurance/consents/v3/consents")
                .operation("POST")
                .build();

        List<Validation> validations = List.of(
                Validation.builder()
                        .violation("Field 'data' is required.")
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
                "http://localhost/process",
                "org-id",
                "1.0.5",
                "sandbox");

        JsonNode payloadNode = objectMapper.readTree("{\"data\":{}}");
        String json = wrapper.toOutboundJson(message, payloadNode);

        assertTrue(json.startsWith("{\n  \"metadata\"") || json.startsWith("{\"metadata\""),
                () -> "metadata must be first field, got: " + json.substring(0, Math.min(json.length(), 80)));

        int metadataIndex = json.indexOf("\"metadata\"");
        int traceIndex = json.indexOf("\"trace\"");
        int requestIndex = json.indexOf("\"request\"");
        int privacyIndex = json.indexOf("\"privacy\"");
        int payloadIndex = json.indexOf("\"payload\"");

        assertTrue(metadataIndex < traceIndex);
        assertTrue(traceIndex < requestIndex);
        assertTrue(requestIndex < privacyIndex);
        assertTrue(privacyIndex < payloadIndex);
        assertTrue(json.contains("\"validations\""));
        assertTrue(json.indexOf("\"validations\"", requestIndex) > requestIndex);
    }
}
