package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.OpenApiPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds and normalizes MOP {@code path} values from transmitter URLs or OpenAPI path templates.
 */
@Component
public class MopPathResolver {

    private static final Logger logger = LoggerFactory.getLogger(MopPathResolver.class);

    private final OpenApiCurrentSpecRegistry specRegistry;

    public MopPathResolver(OpenApiCurrentSpecRegistry specRegistry) {
        this.specRegistry = Objects.requireNonNull(specRegistry, "OpenApiCurrentSpecRegistry cannot be null");
    }

    /**
     * Normalizes a transmitter URL or path and resolves it against modular OpenAPI specs.
     */
    public MopPathResolution resolveFromTransmitterUrl(String urlOrPath) {
        String mopPath = OpenApiPathMatcher.extractOpenInsurancePath(urlOrPath);
        return resolveNormalizedMopPath(mopPath);
    }

    /**
     * Builds a MOP path from {@code basePath + operationPath}, substituting {@code {param}} placeholders,
     * then resolves it against modular specs.
     */
    public MopPathResolution buildFromPathTemplate(
            String basePath,
            String operationPath,
            Map<String, String> pathParameters) {

        String mopPath = OpenApiPathMatcher.buildMopPath(basePath, operationPath, pathParameters);
        return resolveNormalizedMopPath(mopPath);
    }

    private MopPathResolution resolveNormalizedMopPath(String mopPath) {
        Optional<OpenApiSpecResolution> resolution = specRegistry.resolve(mopPath);
        if (resolution.isPresent()) {
            OpenApiSpecResolution spec = resolution.get();
            logger.debug(
                    "Resolved MOP path {} -> spec={} template={}",
                    mopPath,
                    spec.sourceFile(),
                    spec.pathTemplate());
            return new MopPathResolution(
                    mopPath,
                    spec.basePath(),
                    spec.relativePath(),
                    spec.pathTemplate(),
                    spec.sourceFile(),
                    true);
        }

        logger.debug("MOP path {} not matched in swagger/current/ registry", mopPath);
        return new MopPathResolution(mopPath, null, null, null, null, false);
    }
}
