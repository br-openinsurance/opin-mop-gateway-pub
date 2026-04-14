package br.com.opin.mopclient.anonymization.infrastructure.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for wrapping message payloads in JSON structure.
 */
@Component
public class MessagePayloadWrapper {

    private static final String DATA_NODE_NAME = "data";
    private static final String PAYLOAD_NODE_NAME = "payload";
    private static final Logger logger = LoggerFactory.getLogger(MessagePayloadWrapper.class);
    private final ObjectMapper objectMapper;

    public MessagePayloadWrapper() {
        this.objectMapper = new ObjectMapper();
    }

    public String wrap(String originalJson, JsonNode node) {
        try {
            if (originalJson == null || originalJson.isBlank()) {
                logger.warn("Original JSON is null or empty");
                return "{}";
            }

            JsonNode originalNode = objectMapper.readTree(originalJson);
            ObjectNode updatedNode = objectMapper.createObjectNode();

            if (originalNode instanceof ObjectNode) {
                updatedNode.setAll((ObjectNode) originalNode);
            } else {
                logger.warn("Original JSON is not an ObjectNode");
                return originalJson;
            }

            ObjectNode payloadNode;
            JsonNode existingPayload = updatedNode.get(PAYLOAD_NODE_NAME);
            if (existingPayload instanceof ObjectNode) {
                payloadNode = (ObjectNode) existingPayload;
            } else {
                payloadNode = objectMapper.createObjectNode();
                updatedNode.set(PAYLOAD_NODE_NAME, payloadNode);
            }

            if (node == null || node.isEmpty()) {
                logger.info("Node is null or empty, inserting empty object in data field");
                payloadNode.set(DATA_NODE_NAME, objectMapper.createObjectNode());
            } else {
                JsonNode dataNode = node.get(DATA_NODE_NAME);
                if (dataNode != null && !dataNode.isEmpty()) {
                    payloadNode.set(DATA_NODE_NAME, dataNode);
                    logger.info("Data field inserted into payload node");
                } else {
                    logger.info("Field 'data' is missing or empty in the provided node. Using node itself as data");
                    payloadNode.set(DATA_NODE_NAME, node);
                }
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatedNode);

        } catch (Exception e) {
            logger.error("Error while wrapping JSON: {}", e.getMessage(), e);
            return originalJson != null ? originalJson : "{}";
        }
    }
}
