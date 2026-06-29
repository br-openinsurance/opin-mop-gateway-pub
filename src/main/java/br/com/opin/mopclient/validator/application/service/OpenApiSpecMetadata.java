package br.com.opin.mopclient.validator.application.service;

import java.util.Set;

/**
 * Lightweight metadata extracted from a spec YAML ({@code servers} + {@code paths} keys only).
 */
record OpenApiSpecMetadata(String fileName, String basePath, Set<String> pathTemplates) {
}
