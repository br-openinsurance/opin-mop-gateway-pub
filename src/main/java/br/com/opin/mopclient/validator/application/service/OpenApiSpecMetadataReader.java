package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads only {@code servers} and {@code paths} from a spec file — no openapi4j parse.
 */
final class OpenApiSpecMetadataReader {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecMetadataReader.class);
    private static final Yaml YAML = new Yaml();

    private OpenApiSpecMetadataReader() {
    }

    static Optional<OpenApiSpecMetadata> read(Resource resource) {
        String fileName = resource.getFilename();
        if (fileName == null) {
            return Optional.empty();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Object loaded = YAML.load(inputStream);
            if (!(loaded instanceof Map<?, ?> root)) {
                return Optional.empty();
            }
            String basePath = extractBasePath(root);
            Set<String> pathTemplates = extractPathTemplates(root);
            if (pathTemplates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new OpenApiSpecMetadata(fileName, basePath, pathTemplates));
        } catch (Exception e) {
            logger.warn("Failed to read lightweight metadata from {} — skipping index entry", fileName, e);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractBasePath(Map<?, ?> root) {
        Object servers = root.get("servers");
        if (!(servers instanceof List<?> serverList) || serverList.isEmpty()) {
            return "/";
        }
        Object firstServer = serverList.get(0);
        if (!(firstServer instanceof Map<?, ?> serverMap)) {
            return "/";
        }
        Object url = serverMap.get("url");
        return OpenApiPathMatcher.extractBasePath(url != null ? url.toString() : null);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractPathTemplates(Map<?, ?> root) {
        Object paths = root.get("paths");
        if (!(paths instanceof Map<?, ?> pathMap) || pathMap.isEmpty()) {
            return Set.of();
        }
        Set<String> templates = new LinkedHashSet<>();
        for (Object key : pathMap.keySet()) {
            if (key != null) {
                templates.add(key.toString());
            }
        }
        return templates;
    }
}
