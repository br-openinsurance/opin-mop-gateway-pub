package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.FileUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapi4j.parser.OpenApi3Parser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("swagger/current integrity")
class SwaggerCurrentSpecsIntegrityTest {

    private static final String CURRENT_SPECS_PATTERN = "classpath*:swagger/current/*.yaml";

    static Stream<String> swaggerCurrentYamlFiles() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(CURRENT_SPECS_PATTERN);
        assertTrue(resources.length > 0, "expected swagger/current/*.yaml resources on the test classpath");
        return Stream.of(resources)
                .map(Resource::getFilename)
                .filter(fileName -> !OpenApiSpecPhaseCatalog.excludedFromOpenInsuranceValidation(fileName))
                .sorted();
    }

    @Test
    @DisplayName("registry loads every swagger/current yaml without failures")
    void registryLoadsAllSpecs() {
        OpenApiCurrentSpecRegistry registry = new OpenApiCurrentSpecRegistry();
        assertDoesNotThrow(registry::loadAllSpecs);
        assertTrue(registry.loadedSpecFileCount() > 0);
        assertTrue(registry.routeCount() > 0);
        assertTrue(registry.failedSpecFiles().isEmpty(), () -> "failed specs: " + registry.failedSpecFiles());
    }

    @ParameterizedTest(name = "parses {0}")
    @MethodSource("swaggerCurrentYamlFiles")
    @DisplayName("each swagger/current yaml parses with openapi4j")
    void eachYamlParses(String fileName) throws Exception {
        Resource resource = new PathMatchingResourcePatternResolver()
                .getResource("classpath:swagger/current/" + fileName);
        assertTrue(resource.exists(), fileName + " must exist on classpath");

        try (InputStream inputStream = resource.getInputStream()) {
            var openApi = new OpenApi3Parser().parse(FileUtils.inputStreamToFile(inputStream, fileName), true);
            assertNotNull(openApi.getPaths(), fileName + " must define paths");
            assertFalse(openApi.getPaths().isEmpty(), fileName + " must not have an empty paths section");
        }
    }
}
