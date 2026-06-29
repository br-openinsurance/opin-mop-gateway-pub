package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.FileUtils;
import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves modular Open Insurance specs from {@code swagger/current/}.
 * <p>
 * A lightweight path index ({@code basePath + pathTemplate → file}) is built once from YAML metadata.
 * Only the matching spec file is fully parsed (openapi4j) on demand; templates with {@code {param}} are
 * supported via {@link OpenApiPathMatcher#pathTemplateMatches(String, String)}.
 */
@Component
public class OpenApiCurrentSpecRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiCurrentSpecRegistry.class);
    /**
     * {@code classpath*:} scans every classpath root (required for Spring Boot fat JARs).
     */
    private static final String CURRENT_SPECS_PATTERN = "classpath*:swagger/current/*.yaml";

    private final Object loadLock = new Object();
    private final List<RegisteredRoute> routes = new ArrayList<>();
    private final List<String> failedSpecFiles = new ArrayList<>();
    private final Set<String> loadedSpecFileNames = new HashSet<>();
    private final Set<String> failedSpecFileNames = new HashSet<>();

    private Resource[] specResources;
    private OpenApiSpecPathIndex pathIndex;
    private volatile boolean pathIndexBuilt;

    /**
     * Resolves a full MOP path to the best matching spec and relative operation path.
     * Uses the lightweight index to locate the spec file, then parses only that file if needed.
     */
    public Optional<OpenApiSpecResolution> resolve(String mopPath) {
        String normalizedMopPath = OpenApiPathMatcher.normalizePath(mopPath);
        ensurePathIndexBuilt();

        Optional<OpenApiSpecResolution> match = findMatch(normalizedMopPath);
        if (match.isPresent()) {
            return match;
        }

        synchronized (loadLock) {
            match = findMatch(normalizedMopPath);
            if (match.isPresent()) {
                return match;
            }
            Optional<OpenApiSpecPathIndex.IndexedRoute> indexed = pathIndex.findBestMatch(normalizedMopPath);
            if (indexed.isEmpty()) {
                return Optional.empty();
            }
            pathIndex.resourceFor(indexed.get().sourceFile()).ifPresent(this::loadSpecFileIfNeeded);
            return findMatch(normalizedMopPath);
        }
    }

    /**
     * Loads every spec file under {@code swagger/current/}. Intended for tests and diagnostics only;
     * production code should rely on {@link #resolve(String)} for on-demand loading.
     */
    public void loadAllSpecs() {
        synchronized (loadLock) {
            ensurePathIndexBuilt();
            for (Resource resource : specResources) {
                loadSpecFileIfNeeded(resource);
            }
            logger.info(
                    "Modular OpenAPI registry fully loaded: {} spec file(s), {} route(s), {} failure(s)",
                    loadedSpecFileCount(),
                    routeCount(),
                    failedSpecFiles.size());
        }
    }

    public int routeCount() {
        synchronized (routes) {
            return routes.size();
        }
    }

    public int loadedSpecFileCount() {
        synchronized (loadLock) {
            return loadedSpecFileNames.size();
        }
    }

    public int discoveredSpecFileCount() {
        synchronized (loadLock) {
            ensureResourcesDiscovered();
            return specResources.length;
        }
    }

    /**
     * Routes indexed in the lightweight metadata index (all specs, no openapi4j parse).
     */
    public int indexedRouteCount() {
        synchronized (loadLock) {
            ensurePathIndexBuilt();
            return pathIndex.routeCount();
        }
    }

    public List<String> failedSpecFiles() {
        synchronized (loadLock) {
            return Collections.unmodifiableList(failedSpecFiles);
        }
    }

    /**
     * Resolves the Open Insurance phase for a MOP path (wiki-aligned catalog).
     */
    public OpenInsurancePhase phaseForPath(String mopPath) {
        ensurePathIndexBuilt();
        return pathIndex.findBestMatch(OpenApiPathMatcher.normalizePath(mopPath))
                .map(OpenApiSpecPathIndex.IndexedRoute::phase)
                .orElseGet(() -> OpenApiSpecPhaseCatalog.phaseForMopPath(mopPath));
    }

    private void ensurePathIndexBuilt() {
        ensureResourcesDiscovered();
        if (pathIndexBuilt) {
            return;
        }
        synchronized (loadLock) {
            if (pathIndexBuilt) {
                return;
            }
            pathIndex = new OpenApiSpecPathIndex();
            pathIndex.build(specResources);
            pathIndexBuilt = true;
        }
    }

    private void ensureResourcesDiscovered() {
        if (specResources != null) {
            return;
        }
        synchronized (loadLock) {
            if (specResources != null) {
                return;
            }
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
                specResources = resolver.getResources(CURRENT_SPECS_PATTERN);
                if (specResources.length == 0) {
                    throw new IllegalStateException(
                            "No OpenAPI specs found at " + CURRENT_SPECS_PATTERN + " — check swagger/current/ on the classpath");
                }
                logger.debug("Discovered {} OpenAPI spec file(s) at {}", specResources.length, CURRENT_SPECS_PATTERN);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to scan swagger/current/ specs", e);
            }
        }
    }

    private Optional<OpenApiSpecResolution> findMatch(String normalizedMopPath) {
        RegisteredRoute bestMatch = null;
        int bestScore = -1;
        synchronized (routes) {
            for (RegisteredRoute route : routes) {
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
        }
        if (bestMatch == null) {
            return Optional.empty();
        }
        return Optional.of(new OpenApiSpecResolution(
                bestMatch.openApi(),
                bestMatch.basePath(),
                OpenApiPathMatcher.toRelativePath(normalizedMopPath, bestMatch.basePath()),
                bestMatch.pathTemplate(),
                bestMatch.sourceFile(),
                OpenApiSpecPhaseCatalog.phaseForFile(bestMatch.sourceFile())));
    }

    private void loadSpecFileIfNeeded(Resource resource) {
        String fileName = resource.getFilename();
        if (fileName == null || loadedSpecFileNames.contains(fileName) || failedSpecFileNames.contains(fileName)) {
            return;
        }
        loadSpecFile(resource);
    }

    private void loadSpecFile(Resource resource) {
        String fileName = resource.getFilename() != null ? resource.getFilename() : "unknown.yaml";
        try (InputStream inputStream = resource.getInputStream()) {
            OpenApi3 openApi = new OpenApi3Parser().parse(FileUtils.inputStreamToFile(inputStream, fileName), false);
            String basePath = extractBasePathFromSpec(openApi);
            var paths = openApi.getPaths();
            if (paths == null || paths.isEmpty()) {
                failedSpecFileNames.add(fileName);
                failedSpecFiles.add(fileName + " (no paths)");
                logger.warn("OpenAPI spec {} has no paths section", fileName);
                return;
            }
            synchronized (routes) {
                for (String pathTemplate : paths.keySet()) {
                    routes.add(new RegisteredRoute(basePath, pathTemplate, fileName, openApi));
                }
            }
            loadedSpecFileNames.add(fileName);
            logger.debug("Loaded spec {} with basePath={} and {} path(s)", fileName, basePath, paths.size());
        } catch (Exception e) {
            failedSpecFileNames.add(fileName);
            failedSpecFiles.add(fileName + " (" + e.getMessage() + ")");
            logger.warn("Failed to load OpenAPI spec {} — skipping", fileName, e);
        }
    }

    private static String extractBasePathFromSpec(OpenApi3 openApi) {
        List<Server> servers = openApi.getServers();
        if (servers == null || servers.isEmpty() || servers.get(0).getUrl() == null) {
            return "/";
        }
        return OpenApiPathMatcher.extractBasePath(servers.get(0).getUrl());
    }

    private record RegisteredRoute(
            String basePath,
            String pathTemplate,
            String sourceFile,
            OpenApi3 openApi) {
    }
}
