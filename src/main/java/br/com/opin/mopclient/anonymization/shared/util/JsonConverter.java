package br.com.opin.mopclient.anonymization.shared.util;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Converter for JSON operations.
 */
public final class JsonConverter {

    private static final Logger logger = LoggerFactory.getLogger(JsonConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private JsonConverter() {
        // Utility class - prevent instantiation
    }

    public static String toJson(Object obj) {
        try {
            return GSON.toJson(obj);
        } catch (Exception e) {
            logger.error("Error converting object to JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return GSON.fromJson(json, classOfT);
        } catch (Exception e) {
            logger.error("Error parsing JSON to object: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String updateJsonField(String originalJson, String fieldName, String newValue) {
        try {
            JsonNode rootNode = objectMapper.readTree(originalJson);

            if (!(rootNode instanceof ObjectNode objectNode)) {
                logger.warn("JSON root is not an ObjectNode.");
                return newValue;
            }
            objectNode.put(fieldName, newValue);
            return objectMapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            logger.error("Error updating JSON: {}", e.getMessage(), e);
            return "Error updating JSON: " + e.getMessage();
        }
    }

    public static String extractQuotedField(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        int start = input.indexOf('\'');
        if (start == -1) {
            return null;
        }

        int end = input.indexOf('\'', start + 1);
        if (end == -1) {
            return null;
        }

        return input.substring(start + 1, end);
    }

    public static List<Validation> updateValidationsWithFieldName(List<Validation> validations) {
        if (validations == null) {
            return List.of();
        }

        return validations.stream()
                .map(validation -> {
                    validation.setAttribute(extractQuotedField(validation.getViolation()));
                    return validation;
                })
                .toList();
    }
}
