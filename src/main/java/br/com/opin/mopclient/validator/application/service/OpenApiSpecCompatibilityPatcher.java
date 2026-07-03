package br.com.opin.mopclient.validator.application.service;

import org.openapi4j.core.model.v3.OAI3SchemaKeywords;
import org.openapi4j.parser.model.v3.Components;
import org.openapi4j.parser.model.v3.Header;
import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Parameter;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.parser.model.v3.RequestBody;
import org.openapi4j.parser.model.v3.Response;
import org.openapi4j.parser.model.v3.Schema;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Aligns Open Insurance specs with openapi4j validation behaviour.
 * <p>
 * Open Insurance defines several fields as {@code type: string} with {@code format: double|float|int32|int64}
 * (e.g. {@code shareholding} in customers). openapi4j {@code FormatValidator} treats those formats as JSON
 * <strong>numbers</strong>, rejecting string values even when the schema type is {@code string}. This patch
 * removes the numeric {@code format} on string schemas so {@code pattern} and other string constraints still apply.
 */
public final class OpenApiSpecCompatibilityPatcher {

    private static final Set<String> NUMERIC_STRING_FORMATS = Set.of(
            OAI3SchemaKeywords.FORMAT_INT32,
            OAI3SchemaKeywords.FORMAT_INT64,
            OAI3SchemaKeywords.FORMAT_FLOAT,
            OAI3SchemaKeywords.FORMAT_DOUBLE);

    private static final Pattern NUMERIC_FORMAT_FALSE_POSITIVE = Pattern.compile(
            "Value '.*' does not match format '(double|float|int32|int64)'\\.");

    private OpenApiSpecCompatibilityPatcher() {
    }

    /**
     * openapi4j reports code 1007 when a {@code type: string} field uses a numeric {@code format}
     * but the JSON value is textual — common in Open Insurance specs (e.g. {@code shareholding}).
     */
    public static boolean isStringNumericFormatFalsePositive(int code, String message) {
        return code == 1007
                && message != null
                && NUMERIC_FORMAT_FALSE_POSITIVE.matcher(message).matches();
    }

    public static void patch(OpenApi3 openApi) {
        if (openApi == null) {
            return;
        }
        Set<Schema> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        patchComponents(openApi.getComponents(), visited);
        if (openApi.getPaths() != null) {
            for (Path path : openApi.getPaths().values()) {
                patchPath(path, visited);
            }
        }
    }

    private static void patchComponents(Components components, Set<Schema> visited) {
        if (components == null) {
            return;
        }
        if (components.getSchemas() != null) {
            for (Schema schema : components.getSchemas().values()) {
                patchSchema(schema, visited);
            }
        }
        if (components.getResponses() != null) {
            for (Response response : components.getResponses().values()) {
                patchResponse(response, visited);
            }
        }
        if (components.getRequestBodies() != null) {
            for (RequestBody requestBody : components.getRequestBodies().values()) {
                patchRequestBody(requestBody, visited);
            }
        }
        if (components.getParameters() != null) {
            for (Parameter parameter : components.getParameters().values()) {
                patchParameter(parameter, visited);
            }
        }
        if (components.getHeaders() != null) {
            for (Header header : components.getHeaders().values()) {
                patchHeader(header, visited);
            }
        }
    }

    private static void patchPath(Path path, Set<Schema> visited) {
        if (path == null) {
            return;
        }
        if (path.getParameters() != null) {
            for (Parameter parameter : path.getParameters()) {
                patchParameter(parameter, visited);
            }
        }
        if (path.getOperations() == null) {
            return;
        }
        for (Operation operation : path.getOperations().values()) {
            patchOperation(operation, visited);
        }
    }

    private static void patchOperation(Operation operation, Set<Schema> visited) {
        if (operation == null) {
            return;
        }
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                patchParameter(parameter, visited);
            }
        }
        patchRequestBody(operation.getRequestBody(), visited);
        if (operation.getResponses() != null) {
            for (Response response : operation.getResponses().values()) {
                patchResponse(response, visited);
            }
        }
    }

    private static void patchParameter(Parameter parameter, Set<Schema> visited) {
        if (parameter != null && parameter.getSchema() != null) {
            patchSchema(parameter.getSchema(), visited);
        }
    }

    private static void patchHeader(Header header, Set<Schema> visited) {
        if (header != null && header.getSchema() != null) {
            patchSchema(header.getSchema(), visited);
        }
    }

    private static void patchRequestBody(RequestBody requestBody, Set<Schema> visited) {
        if (requestBody == null || requestBody.getContentMediaTypes() == null) {
            return;
        }
        for (MediaType mediaType : requestBody.getContentMediaTypes().values()) {
            patchMediaType(mediaType, visited);
        }
    }

    private static void patchResponse(Response response, Set<Schema> visited) {
        if (response == null || response.getContentMediaTypes() == null) {
            return;
        }
        for (MediaType mediaType : response.getContentMediaTypes().values()) {
            patchMediaType(mediaType, visited);
        }
    }

    private static void patchMediaType(MediaType mediaType, Set<Schema> visited) {
        if (mediaType != null && mediaType.getSchema() != null) {
            patchSchema(mediaType.getSchema(), visited);
        }
    }

    private static void patchSchema(Schema schema, Set<Schema> visited) {
        if (schema == null || !visited.add(schema) || schema.isRef()) {
            return;
        }
        if (OAI3SchemaKeywords.TYPE_STRING.equalsIgnoreCase(schema.getType())
                && schema.getFormat() != null
                && NUMERIC_STRING_FORMATS.contains(schema.getFormat().toLowerCase(Locale.ROOT))) {
            schema.setFormat(null);
        }
        if (schema.getProperties() != null) {
            for (Schema child : schema.getProperties().values()) {
                patchSchema(child, visited);
            }
        }
        patchSchema(schema.getItemsSchema(), visited);
        patchSchema(schema.getNotSchema(), visited);
        patchSchema(schema.getAdditionalProperties(), visited);
        patchSchemaList(schema.getAllOfSchemas(), visited);
        patchSchemaList(schema.getOneOfSchemas(), visited);
        patchSchemaList(schema.getAnyOfSchemas(), visited);
    }

    private static void patchSchemaList(List<Schema> schemas, Set<Schema> visited) {
        if (schemas == null) {
            return;
        }
        for (Schema schema : schemas) {
            patchSchema(schema, visited);
        }
    }
}
