package br.com.opin.mopclient.validator.application.service;

import org.openapi4j.parser.model.v3.OpenApi3;

/**
 * Result of resolving a full MOP {@code path} header to a modular OpenAPI spec in {@code swagger/current/}.
 */
public record OpenApiSpecResolution(
        OpenApi3 openApi,
        String basePath,
        String relativePath,
        String pathTemplate,
        String sourceFile,
        OpenInsurancePhase phase) {
}
