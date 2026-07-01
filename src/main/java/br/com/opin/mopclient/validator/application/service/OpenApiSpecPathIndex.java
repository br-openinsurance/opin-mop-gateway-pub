package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory index mapping MOP paths to spec files via {@code basePath + pathTemplate}.
 * Built from lightweight YAML metadata; supports concrete paths and templates with {@code {param}}.
 */
final class OpenApiSpecPathIndex {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecPathIndex.class);

    private final List<IndexedRoute> routes = new ArrayList<>();
    private final Map<String, Resource> resourceByFileName = new HashMap<>();

    void build(Resource[] specResources) {
        routes.clear();
        resourceByFileName.clear();
        int indexedFiles = 0;
        for (Resource resource : specResources) {
            String fileName = resource.getFilename();
            if (fileName == null) {
                continue;
            }
            resourceByFileName.put(fileName, resource);
            if (OpenApiSpecPhaseCatalog.excludedFromOpenInsuranceValidation(fileName)) {
                continue;
            }
            Optional<OpenApiSpecMetadata> metadata = OpenApiSpecMetadataReader.read(resource);
            if (metadata.isEmpty()) {
                continue;
            }
            indexedFiles++;
            OpenApiSpecMetadata meta = metadata.get();
            OpenInsurancePhase phase = OpenApiSpecPhaseCatalog.phaseForFile(meta.fileName());
            for (String pathTemplate : meta.pathTemplates()) {
                routes.add(new IndexedRoute(meta.basePath(), pathTemplate, meta.fileName(), phase));
            }
        }
        logger.info(
                "OpenAPI path index built: {} route(s) from {} spec file(s) ({} resource(s) on classpath)",
                routes.size(),
                indexedFiles,
                specResources.length);
    }

    int routeCount() {
        return routes.size();
    }

    Optional<IndexedRoute> findBestMatch(String normalizedMopPath) {
        IndexedRoute bestMatch = null;
        int bestScore = -1;
        for (IndexedRoute route : routes) {
            String relativePath = OpenApiPathMatcher.toRelativePath(normalizedMopPath, route.basePath());
            if (relativePath == null) {
                continue;
            }
            if (!OpenApiPathMatcher.pathTemplateMatches(route.pathTemplate(), relativePath)) {
                continue;
            }
            int score = route.basePath().length() + route.pathTemplate().length();
            if (score > bestScore) {
                bestScore = score;
                bestMatch = route;
            }
        }
        return Optional.ofNullable(bestMatch);
    }

    Optional<Resource> resourceFor(String fileName) {
        return Optional.ofNullable(resourceByFileName.get(fileName));
    }

    record IndexedRoute(String basePath, String pathTemplate, String sourceFile, OpenInsurancePhase phase) {
    }
}
