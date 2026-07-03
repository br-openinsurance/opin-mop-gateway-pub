package br.com.opin.mopclient.validator.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.openapi4j.core.model.OAIContext;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.parser.model.v3.RequestBody;
import org.openapi4j.parser.model.v3.Response;
import org.openapi4j.parser.model.v3.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Coerces JSON numbers to strings for OpenAPI fields declared as {@code type: string}
 * (e.g. {@code shareholding}). Participants may send decimals as numbers; openapi4j expects
 * textual nodes for string schemas.
 */
public final class OpenApiStringNumericPayloadCoercer {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiStringNumericPayloadCoercer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String APPLICATION_JSON = "application/json";

    private OpenApiStringNumericPayloadCoercer() {
    }

    public static String coerce(
            OpenApi3 openApi,
            String validationPath,
            String relativePath,
            Request.Method method,
            boolean validateAsResponse,
            int responseStatusCode,
            String jsonPayload) {
        if (jsonPayload == null || jsonPayload.isBlank() || openApi == null) {
            return jsonPayload;
        }
        try {
            Schema bodySchema = resolveBodySchema(
                    openApi, validationPath, relativePath, method, validateAsResponse, responseStatusCode);
            if (bodySchema == null) {
                return jsonPayload;
            }
            JsonNode root = OBJECT_MAPPER.readTree(jsonPayload);
            JsonNode coerced = coerceNode(openApi.getContext(), root, bodySchema);
            return OBJECT_MAPPER.writeValueAsString(coerced);
        } catch (Exception e) {
            logger.debug("Skipping string-numeric payload coercion: {}", e.getMessage());
            return jsonPayload;
        }
    }

    private static Schema resolveBodySchema(
            OpenApi3 openApi,
            String validationPath,
            String relativePath,
            Request.Method method,
            boolean validateAsResponse,
            int responseStatusCode) {
        Path apiPath = OpenApiOperationSupport.resolveApiPath(openApi, validationPath, relativePath);
        Operation operation = OpenApiOperationSupport.resolveOperation(apiPath, method);
        if (operation == null) {
            return null;
        }
        if (validateAsResponse) {
            Response response = operation.getResponse(String.valueOf(responseStatusCode));
            if (response == null && operation.hasResponse("default")) {
                response = operation.getResponse("default");
            }
            return schemaFromResponse(response);
        }
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) {
            return null;
        }
        MediaType mediaType = requestBody.getContentMediaType(APPLICATION_JSON);
        if (mediaType == null && requestBody.getContentMediaTypes() != null
                && !requestBody.getContentMediaTypes().isEmpty()) {
            mediaType = requestBody.getContentMediaTypes().values().iterator().next();
        }
        return mediaType != null ? mediaType.getSchema() : null;
    }

    private static Schema schemaFromResponse(Response response) {
        if (response == null) {
            return null;
        }
        MediaType mediaType = response.getContentMediaType(APPLICATION_JSON);
        if (mediaType == null && response.getContentMediaTypes() != null
                && !response.getContentMediaTypes().isEmpty()) {
            mediaType = response.getContentMediaTypes().values().iterator().next();
        }
        return mediaType != null ? mediaType.getSchema() : null;
    }

    private static JsonNode coerceNode(OAIContext context, JsonNode value, Schema schema) {
        if (value == null || value.isNull() || schema == null) {
            return value;
        }
        Schema flat = resolveFlatSchema(context, schema);
        if (flat == null) {
            return value;
        }

        if (isStringType(flat) && value.isNumber()) {
            return TextNode.valueOf(numberToPlainString(value));
        }

        if (value.isObject()) {
            JsonNode coerced = value;
            if (flat.hasAllOfSchemas()) {
                for (Schema branch : flat.getAllOfSchemas()) {
                    coerced = coerceObjectProperties(context, coerced, branch);
                }
            }
            if (flat.getProperties() != null) {
                coerced = coerceObjectProperties(context, coerced, flat);
            }
            return coerced;
        }

        if (value.isArray() && flat.getItemsSchema() != null) {
            ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
            for (JsonNode item : value) {
                arrayNode.add(coerceNode(context, item, flat.getItemsSchema()));
            }
            return arrayNode;
        }

        return value;
    }

    private static JsonNode coerceObjectProperties(OAIContext context, JsonNode value, Schema objectSchema) {
        Schema flat = resolveFlatSchema(context, objectSchema);
        if (flat == null || flat.getProperties() == null || !value.isObject()) {
            return value;
        }
        ObjectNode copy = value.deepCopy();
        for (var entry : flat.getProperties().entrySet()) {
            String property = entry.getKey();
            if (copy.has(property)) {
                copy.set(property, coerceNode(context, copy.get(property), entry.getValue()));
            }
        }
        return copy;
    }

    private static Schema resolveFlatSchema(OAIContext context, Schema schema) {
        if (schema == null) {
            return null;
        }
        if (schema.isRef() && context != null) {
            return schema.getFlatSchema(context);
        }
        return schema;
    }

    private static boolean isStringType(Schema schema) {
        return schema != null && "string".equalsIgnoreCase(schema.getType());
    }

    private static String numberToPlainString(JsonNode numberNode) {
        if (numberNode.isIntegralNumber()) {
            return numberNode.bigIntegerValue().toString();
        }
        return new BigDecimal(numberNode.asText()).stripTrailingZeros().toPlainString();
    }
}
