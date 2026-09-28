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
 * Resolves modular Open Insurance specs from {@code swagger/current/}, then {@code swagger/version/}.
 * <p>
 * Newest specs in {@code current/} are tried first. If the MOP path does not match
 * ({@code Operation path not found}), older official specs in {@code version/} are used as fallback.
 */
@Component
public class OpenApiCurrentSpecRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiCurrentSpecRegistry.class);
    /**
     * {@code classpath*:} scans every classpath root (required for Spring Boot fat JARs).
     */
    private static final String CURRENT_SPECS_PATTERN = "classpath*:swagger/current/*.yaml";
    private static final String VERSION_SPECS_PATTERN = "classpath*:swagger/version/*.yaml";
    private static final String CURRENT_LOAD_PREFIX = "current:";
    private static final String VERSION_LOAD_PREFIX = "version:";

    private final Object loadLock = new Object();
    private final List<RegisteredRoute> routes = new ArrayList<>();
    private final List<String> failedSpecFiles = new ArrayList<>();
    private final Set<String> loadedSpecKeys = new HashSet<>();
    private final Set<String> failedSpecKeys = new HashSet<>();

    private Resource[] currentSpecResources;
    private Resource[] versionSpecResources;
    private OpenApiSpecPathIndex currentPathIndex;
    private OpenApiSpecPathIndex versionPathIndex;
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
            Optional<OpenApiSpecPathIndex.IndexedRoute> currentIndexed = currentPathIndex.findBestMatch(normalizedMopPath);
            if (currentIndexed.isPresent()) {
                currentPathIndex.resourceFor(currentIndexed.get().sourceFile())
                        .ifPresent(resource -> loadSpecFileIfNeeded(resource, CURRENT_LOAD_PREFIX));
                match = findMatch(normalizedMopPath);
                if (match.isPresent()) {
                    return match;
                }
            }
            Optional<OpenApiSpecPathIndex.IndexedRoute> versionIndexed = versionPathIndex.findBestMatch(normalizedMopPath);
            if (versionIndexed.isEmpty()) {
                return Optional.empty();
            }
            logger.info(
                    "OpenAPI path not found in swagger/current/; falling back to swagger/version/ | mopPath={} | spec={}",
                    normalizedMopPath,
                    versionIndexed.get().sourceFile());
            versionPathIndex.resourceFor(versionIndexed.get().sourceFile())
                    .ifPresent(resource -> loadSpecFileIfNeeded(resource, VERSION_LOAD_PREFIX));
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
            for (Resource resource : currentSpecResources) {
                loadSpecFileIfNeeded(resource, CURRENT_LOAD_PREFIX);
            }
            for (Resource resource : versionSpecResources) {
                loadSpecFileIfNeeded(resource, VERSION_LOAD_PREFIX);
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
            return loadedSpecKeys.size();
        }
    }

    public int discoveredSpecFileCount() {
        synchronized (loadLock) {
            ensureResourcesDiscovered();
            return currentSpecResources.length + versionSpecResources.length;
        }
    }

    /**
     * Routes indexed in the lightweight metadata index (all specs, no openapi4j parse).
     */
    public int indexedRouteCount() {
        synchronized (loadLock) {
            ensurePathIndexBuilt();
            return currentPathIndex.routeCount() + versionPathIndex.routeCount();
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
        String normalized = OpenApiPathMatcher.normalizePath(mopPath);
        return currentPathIndex.findBestMatch(normalized)
                .or(() -> versionPathIndex.findBestMatch(normalized))
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
            currentPathIndex = new OpenApiSpecPathIndex();
            currentPathIndex.build(currentSpecResources);
            versionPathIndex = new OpenApiSpecPathIndex();
            versionPathIndex.build(versionSpecResources);
            pathIndexBuilt = true;
        }
    }

    private void ensureResourcesDiscovered() {
        if (currentSpecResources != null) {
            return;
        }
        synchronized (loadLock) {
            if (currentSpecResources != null) {
                return;
            }
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
                currentSpecResources = resolver.getResources(CURRENT_SPECS_PATTERN);
                if (currentSpecResources.length == 0) {
                    throw new IllegalStateException(
                            "No OpenAPI specs found at " + CURRENT_SPECS_PATTERN + " — check swagger/current/ on the classpath");
                }
                try {
                    versionSpecResources = resolver.getResources(VERSION_SPECS_PATTERN);
                } catch (Exception versionScan) {
                    logger.warn("Failed to scan swagger/version/ — older-spec fallback disabled", versionScan);
                    versionSpecResources = new Resource[0];
                }
                logger.debug(
                        "Discovered {} OpenAPI spec file(s) at {} and {} older spec file(s) at {}",
                        currentSpecResources.length,
                        CURRENT_SPECS_PATTERN,
                        versionSpecResources.length,
                        VERSION_SPECS_PATTERN);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to scan swagger/current/ or swagger/version/ specs", e);
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

    private void loadSpecFileIfNeeded(Resource resource, String loadPrefix) {
        String fileName = resource.getFilename();
        if (fileName == null) {
            return;
        }
        String loadKey = loadPrefix + fileName;
        if (OpenApiSpecPhaseCatalog.excludedFromOpenInsuranceValidation(fileName)
                || loadedSpecKeys.contains(loadKey)
                || failedSpecKeys.contains(loadKey)) {
            return;
        }
        loadSpecFile(resource, loadKey);
    }

    private void loadSpecFile(Resource resource, String loadKey) {
        String fileName = resource.getFilename() != null ? resource.getFilename() : "unknown.yaml";
        try (InputStream inputStream = resource.getInputStream()) {
            OpenApi3 openApi = new OpenApi3Parser().parse(FileUtils.inputStreamToFile(inputStream, fileName), false);
            OpenApiSpecCompatibilityPatcher.patch(openApi);
            OpenApiSpecFileVersion.applyToParsedSpec(openApi, fileName);
            String basePath = OpenApiSpecFileVersion.applyToBasePath(fileName, extractBasePathFromSpec(openApi));
            var paths = openApi.getPaths();
            if (paths == null || paths.isEmpty()) {
                failedSpecKeys.add(loadKey);
                failedSpecFiles.add(fileName + " (no paths)");
                logger.warn("OpenAPI spec {} has no paths section", fileName);
                return;
            }
            synchronized (routes) {
                for (String pathTemplate : paths.keySet()) {
                    routes.add(new RegisteredRoute(basePath, pathTemplate, fileName, openApi));
                }
            }
            loadedSpecKeys.add(loadKey);
            logger.debug("Loaded spec {} with basePath={} and {} path(s)", fileName, basePath, paths.size());
        } catch (Exception e) {
            failedSpecKeys.add(loadKey);
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
