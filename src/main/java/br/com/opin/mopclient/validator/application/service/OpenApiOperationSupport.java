package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.Response;
import org.openapi4j.operation.validator.validation.OperationValidator;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.schema.validator.ValidationData;

/**
 * Resolves OpenAPI operations from path + HTTP method and validates responses via {@link OperationValidator}.
 */
final class OpenApiOperationSupport {

    private OpenApiOperationSupport() {
    }

    static Operation resolveOperation(Path path, Request.Method method) {
        if (path == null) {
            return null;
        }
        return switch (method) {
            case GET -> path.getGet();
            case POST -> path.getPost();
            case PUT -> path.getPut();
            case PATCH -> path.getPatch();
            case DELETE -> path.getDelete();
            case HEAD -> path.getHead();
            case OPTIONS -> path.getOptions();
            default -> null;
        };
    }

    static Path resolveApiPath(OpenApi3 openApi, String pathUrl) {
        if (openApi.getPaths() == null || pathUrl == null || pathUrl.isBlank()) {
            return null;
        }
        if (openApi.getPaths().get(pathUrl) != null) {
            return openApi.getPath(pathUrl);
        }
        for (String template : openApi.getPaths().keySet()) {
            if (OpenApiPathMatcher.pathTemplateMatches(template, pathUrl)) {
                return openApi.getPath(template);
            }
        }
        return null;
    }

    static Path resolveApiPath(OpenApi3 openApi, String pathTemplate, String relativePath) {
        Path path = resolveApiPath(openApi, pathTemplate);
        if (path == null && relativePath != null && !relativePath.equals(pathTemplate)) {
            path = resolveApiPath(openApi, relativePath);
        }
        return path;
    }

    /**
     * Ensures the OpenAPI spec defines {@code method} for {@code validationPath}.
     *
     * @param validationPath path key used in the OpenAPI spec (relative or full)
     * @param mopPath        full MOP path shown in validation messages
     */
    static void ensureOperationExists(
            OpenApi3 openApi,
            String validationPath,
            String relativePath,
            Request.Method method,
            String mopPath) {

        Path apiPath = resolveApiPath(openApi, validationPath, relativePath);
        if (apiPath == null) {
            throw new OpenApiOperationResolutionException(
                    "Operation path not found from URL '" + mopPath + "'"
                            + " (OpenAPI template: '" + validationPath + "').",
                    OpenApiOperationResolutionException.CODE_NOT_FOUND);
        }

        Operation operation = resolveOperation(apiPath, method);
        if (operation == null) {
            throw new OpenApiOperationResolutionException(
                    "No " + method + " operation for path '" + mopPath + "'. "
                            + "Check that headers 'path' and 'operation' match the OpenAPI spec "
                            + "(e.g. POST /open-insurance/consents/v3/consents for create consent).",
                    OpenApiOperationResolutionException.CODE_NOT_FOUND);
        }
    }

    /**
     * Validates an HTTP response body against the OpenAPI operation schema.
     *
     * @return validation data; check {@link ValidationData#isValid()} for the outcome
     */
    static ValidationData<Void> validateResponse(
            OpenApi3 openApi,
            String validationPath,
            Request.Method method,
            Response response) {

        Path apiPath = resolveApiPath(openApi, validationPath);
        Operation operation = resolveOperation(apiPath, method);

        OperationValidator validator = new OperationValidator(openApi, apiPath, operation);
        ValidationData<Void> data = new ValidationData<>();
        validator.validateResponse(response, data);
        return data;
    }
}
