package br.com.opin.mopclient.anonymization.domain.service;

import br.com.opin.mopclient.anonymization.shared.util.MopReportidManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JsonAnonymizer {

    private static final Logger logger = LoggerFactory.getLogger(JsonAnonymizer.class);
    private static final TextNode EMPTY_TEXT = TextNode.valueOf("");

    private final ObjectMapper objectMapper;
    private final DataEncryptor encryptor;

    public JsonAnonymizer(ObjectMapper objectMapper, DataEncryptor encryptor) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.encryptor = Objects.requireNonNull(encryptor, "DataEncryptor cannot be null");
    }

    public String anonymize(String jsonInput,
                            Set<String> anonymizedFields,
                            Set<String> exposedFields) {

        String mopReportid = MopReportidManager.getMopReportid();

        if (jsonInput == null || jsonInput.isBlank()) {
            logger.debug("[DEBUG] Empty JSON input | MOP Report ID: {}", mopReportid);
            return "{}";
        }

        int originalLength = jsonInput.length();
        jsonInput = sanitizeJson(jsonInput);

        if (jsonInput.length() != originalLength) {
            logger.warn(
                    "[WARNING] Problematic characters removed from JSON | MOP Report ID: {} | Original size: {} | Cleaned size: {}",
                    mopReportid,
                    originalLength,
                    jsonInput.length()
            );
        }

        try {
            JsonNode root = objectMapper.readTree(jsonInput);
            Set<String> anonymized = normalizeAndCopy(anonymizedFields);
            Set<String> exposed = normalizeAndCopy(exposedFields);
            detectConflicts(anonymized, exposed, mopReportid);

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "[DEBUG] Starting anonymization | MOP Report ID: {} | Anonymized fields: {} | Exposed fields: {}",
                        mopReportid,
                        anonymized,
                        exposed
                );
            }

            JsonNode result = anonymizeNode(root, anonymized, exposed, "", false, mopReportid);
            String resultJson = objectMapper.writeValueAsString(result);

            logger.debug(
                    "[DEBUG] Anonymization completed successfully | MOP Report ID: {}",
                    mopReportid
            );

            return resultJson;

        } catch (Exception e) {
            logger.error(
                    "[ERROR] Anonymization failed. Payload blocked | MOP Report ID: {} | Error: {}",
                    mopReportid,
                    e.getMessage(),
                    e
            );
            return "{}";
        }
    }

    private String sanitizeJson(String json) {
        return json
                .replaceAll("\\u0000", "")
                .replaceAll("\\p{Cntrl}&&[^\r\n\t]", "");
    }

    private Set<String> normalizeAndCopy(Set<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Collections.emptySet();
        }
        return fields.stream()
                .filter(Objects::nonNull)
                .map(f -> f.trim().toLowerCase())
                .collect(Collectors.toUnmodifiableSet());
    }

    private void detectConflicts(Set<String> anonymized, Set<String> exposed, String mopReportid) {
        Set<String> conflicts = anonymized.stream()
                .filter(exposed::contains)
                .collect(Collectors.toSet());

        if (!conflicts.isEmpty()) {
            logger.warn(
                    "[WARNING] Fields present in both anonymized and exposed sets. Anonymization will take priority. | MOP Report ID: {} | Conflicting fields: {}",
                    mopReportid,
                    conflicts
            );
        }
    }

    private JsonNode anonymizeNode(JsonNode node,
                                   Set<String> anonymizedFields,
                                   Set<String> exposedFields,
                                   String currentPath,
                                   boolean underExposedPath,
                                   String mopReportid) {

        if (node == null || node.isNull()) {
            return EMPTY_TEXT;
        }

        try {
            if (node.isObject()) {
                ObjectNode result = objectMapper.createObjectNode();

                node.fields().forEachRemaining(entry -> {
                    String field = entry.getKey();
                    JsonNode value = entry.getValue();
                    String normalizedField = field.toLowerCase();
                    String fieldPath = currentPath.isEmpty() ? field : currentPath + "." + field;

                    try {
                        if (anonymizedFields.contains(normalizedField)) {
                            String rawValue = extractPlainValue(value);
                            String encrypted = encryptor.encrypt(rawValue);
                            result.put(field, encrypted);

                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                        "[TRACE] Field anonymized | Path: {} | MOP Report ID: {}",
                                        fieldPath,
                                        mopReportid
                                );
                            }
                            return;
                        }

                        if (exposedFields.contains(normalizedField)) {
                            boolean childUnderExposed = true;
                            result.set(
                                    field,
                                    value.isContainerNode()
                                            ? anonymizeNode(value, anonymizedFields, exposedFields, fieldPath, childUnderExposed, mopReportid)
                                            : value
                            );

                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                        "[TRACE] Field exposed | Path: {} | MOP Report ID: {}",
                                        fieldPath,
                                        mopReportid
                                );
                            }
                            return;
                        }

                        result.set(
                                field,
                                value.isContainerNode()
                                        ? anonymizeNode(value, anonymizedFields, exposedFields, fieldPath, false, mopReportid)
                                        : EMPTY_TEXT
                        );

                    } catch (Exception e) {
                        logger.error(
                                "[ERROR] Failed to process field | Path: {} | MOP Report ID: {} | Error: {}",
                                fieldPath,
                                mopReportid,
                                e.getMessage(),
                                e
                        );
                        result.set(field, EMPTY_TEXT);
                    }
                });

                return result;
            }

            if (node.isArray()) {
                ArrayNode array = objectMapper.createArrayNode();
                int index = 0;

                for (JsonNode item : node) {
                    String arrayPath = currentPath + "[" + index + "]";
                    try {
                        array.add(anonymizeNode(item, anonymizedFields, exposedFields, arrayPath, underExposedPath, mopReportid));
                    } catch (Exception e) {
                        logger.error(
                                "[ERROR] Failed to process array item | Path: {} | MOP Report ID: {} | Error: {}",
                                arrayPath,
                                mopReportid,
                                e.getMessage(),
                                e
                        );
                        array.add(EMPTY_TEXT);
                    }
                    index++;
                }

                return array;
            }

            if (underExposedPath) {
                return node;
            }
            return EMPTY_TEXT;

        } catch (Exception e) {
            logger.error(
                    "[ERROR] Node anonymization failed. Node cleared | Path: {} | MOP Report ID: {} | Error: {}",
                    currentPath,
                    mopReportid,
                    e.getMessage(),
                    e
            );
            return EMPTY_TEXT;
        }
    }

    private String extractPlainValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }

        if (value.isValueNode()) {
            return value.asText("");
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            logger.warn("[WARNING] Failed to extract plain value, returning empty string", e);
            return "";
        }
    }
}
