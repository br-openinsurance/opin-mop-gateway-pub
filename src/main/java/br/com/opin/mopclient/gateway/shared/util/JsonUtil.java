package br.com.opin.mopclient.gateway.shared.util;


import br.com.opin.mopclient.gateway.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Utility class for searching JSON structures for occurrences of specified keys.
 * Maintains the original JSON structure, replaces non-matching fields with empty values,
 * and returns both the extracted key-value pairs and the modified JSON.
 */
public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static boolean isValidJson(String jsonString) {
        try {
            // Validate input
            if (jsonString == null || jsonString.isBlank()) {
                throw new IllegalArgumentException("Invalid input: JSON string cannot be null or empty.");
            }
            // Attempt parsing the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            return jsonNode != null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Invalid JSON format: " + e.getMessage());
            return false;
        }
    }





    public static String convertJsonNodeToString(JsonNode jsonNode) {
        try {
            if (jsonNode == null) {
                logger.warn("JSON Node is null. Returning empty JSON.");
                return "{}";
            }
            String jsonString = objectMapper.writeValueAsString(jsonNode);
            logger.info("JSON conversion successful: {}", jsonString);
            return jsonString;

        } catch (Exception e) {
            logger.error("Failed to convert JsonNode to string - Cause: {}", e.getMessage(), e);
            return "{}"; // Returns empty JSON on conversion failure
        }

    }


    public static Set<Map.Entry<String, String>> findValuesByKeys(String jsonString, Set<String> keys) {
        try {
            logger.trace("Searching JSON by keys: {}", keys);
            if (jsonString == null || jsonString.isBlank() || keys == null || keys.isEmpty()) {
                throw new BusinessException("Invalid input: JSON and keys cannot be null or empty.");
            }
            Objects.requireNonNull(jsonString, "JSON string cannot be null or empty.");
            Objects.requireNonNull(keys, "Keys cannot be null or empty.");

            JsonNode rootNode = objectMapper.readTree(jsonString);
            Map<String, String> foundValues = new HashMap<>();
            extractValues(rootNode, keys, foundValues);

            return foundValues.entrySet();

        } catch (Exception e) {
            logger.error("Error during JSON search. Cause: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }


    private static void extractValues(JsonNode node, Set<String> keys, Map<String, String> foundValues) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if (keys.contains(entry.getKey())) {
                    foundValues.put(entry.getKey(), entry.getValue().asText());
                }
                extractValues(entry.getValue(), keys, foundValues);
            });
        } else if (node.isArray()) {
            node.forEach(item -> extractValues(item, keys, foundValues));
        }
    }


    public static String modifyJsonStructureBykeys(String jsonString, Set<String> keys) {
        try {
            logger.trace("Modifying JSON structure based on keys: {}", keys);

            Objects.requireNonNull(jsonString, "JSON string cannot be null or empty.");
            Objects.requireNonNull(keys, "Keys cannot be null or empty.");

            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode modifiedJson = filterJsonStructure(rootNode, keys);

            return objectMapper.writeValueAsString(modifiedJson);

        } catch (Exception e) {
            logger.error("Error modifying JSON structure. Cause: {}", e.getMessage(), e);
            return "{}"; // Returns empty JSON on failure
        }
    }

//###
    private static JsonNode filterJsonStructure(JsonNode node, Set<String> keys) {
        if (node.isObject()) {
            ObjectNode modifiedObject = objectMapper.createObjectNode(); // Use ObjectNode explicitly

            node.fields().forEachRemaining(entry -> {
                if (keys.contains(entry.getKey())) {
                    modifiedObject.set(entry.getKey(), filterJsonStructure(entry.getValue(), keys)); // Modify object directly
                } else {
                    modifiedObject.putNull(entry.getKey()); // Avoid reassignment
                }
            });

            return modifiedObject;
        }

        return node;
    }


    /**
     * Resets all values in a JSON while keeping its original structure.
     * Nested objects and arrays are preserved.
     *
     * @param jsonString The original JSON as a string.
     * @return JSON with all field values set to empty strings or `null`.
     */
    public static String resetJsonValues(String jsonString) {
        try {
            Objects.requireNonNull(jsonString, "JSON string cannot be null or empty.");

            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode modifiedJson = processNode(rootNode);

            return objectMapper.writeValueAsString(modifiedJson);

        } catch (Exception e) {
            return "{}"; // Returns empty JSON on failure
        }
    }

    /**
     * Recursively processes the JSON structure, replacing field values with empty strings or null.
     *
     * @param node The JSON node to modify.
     * @return A modified JSON node preserving its structure.
     */
    private static JsonNode processNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode modifiedObject = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                if (entry.getValue().isObject() || entry.getValue().isArray()) {
                    modifiedObject.set(entry.getKey(), processNode(entry.getValue())); // Preserve nested structure
                } else {
                    modifiedObject.put(entry.getKey(), ""); // Set field values to empty string
                }
            });
            return modifiedObject;
        } else if (node.isArray()) {
            return objectMapper.valueToTree(node); // Preserve arrays
        }

        return node;
    }

        /**
         * Converts a Java object to its JSON string representation.
         *
         * @param object the object to convert
         * @return JSON string or error message
         */
        public static String toJson(Object object) {
            try {

                return objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                // Log the error or rethrow as a runtime exception if preferred
                return "{\"error\": \"Failed to serialize object to JSON: " + e.getMessage() + "\"}";
            }
        }

    public static String toJson1(String object) {
        try {

            ObjectMapper mapper = new ObjectMapper();


            String cleanedJson = mapper.readValue(object, String.class);



            return objectMapper.writeValueAsString(cleanedJson);
        } catch (JsonProcessingException e) {
            // Log the error or rethrow as a runtime exception if preferred
            return "{\"error\": \"Failed to serialize object to JSON: " + e.getMessage() + "\"}";
        }
    }


}