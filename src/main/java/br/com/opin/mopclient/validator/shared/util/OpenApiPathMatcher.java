package br.com.opin.mopclient.validator.shared.util;

import java.net.URI;
import java.util.Arrays;

/**
 * Utilities for MOP path resolution: {@code path_MOP = basePath + operationPath}.
 */
public final class OpenApiPathMatcher {

    public static final String OPEN_INSURANCE_PREFIX = "/open-insurance/";

    private OpenApiPathMatcher() {
    }

    /**
     * Normalizes a URL or path for Open Insurance validation, keeping only the segment from
     * {@value #OPEN_INSURANCE_PREFIX} onward (host and any prefix before Open Insurance are removed).
     */
    public static String extractOpenInsurancePath(String urlOrPath) {
        String path = extractPathOnly(urlOrPath);
        int index = path.indexOf(OPEN_INSURANCE_PREFIX);
        if (index >= 0) {
            return normalizePath(path.substring(index));
        }
        return normalizePath(path);
    }

    /**
     * Strips scheme and host when {@code urlOrPath} is a full URL; otherwise normalizes as a path.
     */
    /**
     * Returns the HTTP verb when {@code urlOrPath} starts with {@code GET }, {@code POST }, etc.
     */
    public static java.util.Optional<String> extractLeadingHttpMethod(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            return java.util.Optional.empty();
        }
        String value = urlOrPath.trim();
        int space = value.indexOf(' ');
        if (space <= 0) {
            return java.util.Optional.empty();
        }
        String token = value.substring(0, space).trim().toUpperCase();
        return isHttpMethodToken(token) ? java.util.Optional.of(token) : java.util.Optional.empty();
    }

    public static String extractPathOnly(String urlOrPath) {
        if (urlOrPath == null) {
            return null;
        }
        String value = urlOrPath.trim();
        if (value.isEmpty()) {
            return value;
        }
        value = stripLeadingHttpMethodPrefix(value);
        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                URI uri = URI.create(value);
                if (uri.getPath() != null && !uri.getPath().isBlank()) {
                    return uri.getPath();
                }
            }
        } catch (Exception ignored) {
            // keep original value
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String stripLeadingHttpMethodPrefix(String value) {
        return extractLeadingHttpMethod(value)
                .map(method -> value.substring(method.length()).trim())
                .orElse(value);
    }

    private static boolean isHttpMethodToken(String token) {
        return switch (token) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE" -> true;
            default -> false;
        };
    }

    /**
     * Extracts the path component from an OpenAPI {@code servers.url} value.
     */
    public static String extractBasePath(String serverUrl) {
        if (serverUrl == null || serverUrl.isBlank()) {
            return "/";
        }
        String value = serverUrl.trim();
        if (!value.contains("://")) {
            value = "https://placeholder" + (value.startsWith("/") ? value : "/" + value);
        }
        try {
            URI uri = URI.create(value);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return "/";
            }
            return normalizePath(path);
        } catch (Exception e) {
            return normalizePath(value.startsWith("/") ? value : "/" + value);
        }
    }

    /**
     * Normalizes a URL path for OpenAPI routing (leading slash, no trailing slash except root).
     */
    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String value = path.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Returns the suffix of {@code mopPath} after {@code basePath}, or {@code "/"} when equal.
     */
    public static String toRelativePath(String mopPath, String basePath) {
        String normalizedMop = normalizePath(mopPath);
        String normalizedBase = normalizePath(basePath);
        if (normalizedMop.equals(normalizedBase)) {
            return "/";
        }
        if (!normalizedMop.startsWith(normalizedBase + "/")) {
            return null;
        }
        return normalizedMop.substring(normalizedBase.length());
    }

    /**
     * Replaces {@code {paramName}} placeholders in an OpenAPI path template with concrete values.
     */
    public static String substitutePathParameters(String pathTemplate, java.util.Map<String, String> parameters) {
        if (pathTemplate == null || pathTemplate.isBlank()) {
            return pathTemplate;
        }
        String resolved = pathTemplate;
        if (parameters != null) {
            for (var entry : parameters.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return normalizePath(resolved);
    }

    /**
     * Builds a full MOP path from basePath and operationPath, substituting path parameters when provided.
     */
    public static String buildMopPath(String basePath, String operationPath, java.util.Map<String, String> parameters) {
        String normalizedBase = normalizePath(basePath);
        String relativePath = substitutePathParameters(operationPath, parameters);
        if (relativePath == null || relativePath.isBlank() || "/".equals(relativePath)) {
            return normalizedBase;
        }
        return normalizePath(normalizedBase + relativePath);
    }

    /**
     * Matches an OpenAPI path template (e.g. {@code /consents/{consentId}}) against a concrete path.
     */
    public static boolean pathTemplateMatches(String template, String actual) {
        if (template == null || actual == null) {
            return false;
        }
        String normalizedTemplate = normalizePath(template);
        String normalizedActual = normalizePath(actual);
        if (normalizedTemplate.equals(normalizedActual)) {
            return true;
        }
        String[] templateParts = splitPath(normalizedTemplate);
        String[] actualParts = splitPath(normalizedActual);
        if (templateParts.length != actualParts.length) {
            return false;
        }
        for (int i = 0; i < templateParts.length; i++) {
            String segment = templateParts[i];
            if (isPathParameter(segment)) {
                continue;
            }
            if (!segment.equals(actualParts[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPathParameter(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String[] splitPath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return new String[0];
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(trimmed.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
