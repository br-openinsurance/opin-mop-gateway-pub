package br.com.opin.mopclient.gateway.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Service for parsing JSON payloads from HTTP requests.
 * <p>
 * Handles parsing of request bodies, treating null/empty/invalid JSON
 * as empty JSON objects ({}) to allow requests to proceed.
 */
@Service
public class JsonPayloadParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPayloadParser.class);
    private final ObjectMapper objectMapper;

    private static final String EMPTY_OBJECT_JSON = "{}";

    public JsonPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    }

    /**
     * Normalizes null, empty or whitespace-only bodies to {@code "{}"}.
     */
    public String normalizeRequestBody(String requestBody) {
        if (!StringUtils.hasText(requestBody)) {
            return EMPTY_OBJECT_JSON;
        }
        return requestBody.trim();
    }

    /**
     * Parses JSON body from request.
     * Returns empty object ({}) if body is null, empty or invalid.
     *
     * @param requestBody The request body as string
     * @return JsonNode representing the parsed JSON, or empty object if invalid
     */
    public JsonNode parse(String requestBody) {
        return parseNormalized(normalizeRequestBody(requestBody));
    }

    private JsonNode parseNormalized(String requestBody) {
        if (EMPTY_OBJECT_JSON.equals(requestBody)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(requestBody);
        } catch (JsonProcessingException e) {
            LOGGER.debug("Failed to parse JSON, using empty object: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Converts JsonNode to JSON string.
     *
     * @param jsonNode The JsonNode to convert
     * @return JSON string representation
     * @throws JsonProcessingException if conversion fails
     */
    public String toJsonString(JsonNode jsonNode) throws JsonProcessingException {
        return objectMapper.writeValueAsString(jsonNode);
    }
}

