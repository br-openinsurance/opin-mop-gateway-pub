package br.com.opin.mopclient.anonymization.infrastructure.mapper;

import br.com.opin.mopclient.anonymization.interfaces.dto.message.MessageDTO;
import br.com.opin.mopclient.anonymization.interfaces.dto.message.PayloadDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Serializes {@link MessageDTO} for outbound delivery to the MOP server.
 */
@Component
public class MessagePayloadWrapper {

    private static final String DATA_NODE_NAME = "data";
    private static final Logger logger = LoggerFactory.getLogger(MessagePayloadWrapper.class);
    private final ObjectMapper messageMapper;

    public MessagePayloadWrapper(ObjectMapper objectMapper) {
        this.messageMapper = objectMapper.copy()
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public String toOutboundJson(MessageDTO message, JsonNode anonymizedPayload) {
        try {
            applyPayloadData(message, anonymizedPayload);
            return messageMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing MessageDTO: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize MessageDTO for MOP delivery", e);
        }
    }

    private void applyPayloadData(MessageDTO message, JsonNode anonymizedPayload) {
        PayloadDTO payload = message.getPayload();
        if (payload == null) {
            payload = PayloadDTO.builder().build();
            message.setPayload(payload);
        }
        payload.setData(resolvePayloadData(anonymizedPayload));
    }

    private JsonNode resolvePayloadData(JsonNode node) {
        if (node == null || node.isEmpty()) {
            return messageMapper.createObjectNode();
        }
        JsonNode dataNode = node.get(DATA_NODE_NAME);
        if (dataNode != null && !dataNode.isEmpty()) {
            return dataNode;
        }
        return node;
    }
}
