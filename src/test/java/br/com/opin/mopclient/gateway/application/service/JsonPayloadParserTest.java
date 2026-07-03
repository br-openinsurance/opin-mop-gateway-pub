package br.com.opin.mopclient.gateway.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonPayloadParser")
class JsonPayloadParserTest {

    private JsonPayloadParser parser;

    @BeforeEach
    void setUp() {
        parser = new JsonPayloadParser(new ObjectMapper());
    }

    @Test
    @DisplayName("normalizeRequestBody returns {} for null, empty and whitespace")
    void normalizeRequestBodyReturnsEmptyObject() {
        assertEquals("{}", parser.normalizeRequestBody(null));
        assertEquals("{}", parser.normalizeRequestBody(""));
        assertEquals("{}", parser.normalizeRequestBody("   "));
    }

    @Test
    @DisplayName("parse treats null body as empty object")
    void parseNullBodyAsEmptyObject() throws Exception {
        assertTrue(parser.parse(null).isObject());
        assertTrue(parser.parse(null).isEmpty());
        assertEquals("{}", parser.toJsonString(parser.parse(null)));
    }
}
