package br.com.opin.mopclient.validator.infrastructure.config;

import br.com.opin.mopclient.validator.shared.util.FileUtils;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Configuration for loading and parsing the OpenAPI specification.
 */
@Configuration
public class OpenApiSpecificationConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiSpecificationConfig.class);
    private static final String SWAGGER_FILE_PATH = "swagger/swagger.yaml";

    @Bean
    @Cacheable(value = "openApiSpec")
    public OpenApi3 openApiSpec() throws Exception {
        logger.info("Loading OpenAPI specification from file: {}", SWAGGER_FILE_PATH);
        
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SWAGGER_FILE_PATH);
        if (inputStream == null) {
            String errorMessage = String.format("swagger.yaml file not found in classpath: %s", SWAGGER_FILE_PATH);
            logger.error(errorMessage);
            throw new FileNotFoundException(errorMessage);
        }

        try {
            File specFile = FileUtils.inputStreamToFile(inputStream, "swagger");
            OpenApi3 openApi = new OpenApi3Parser().parse(specFile, false);
            logger.info("OpenAPI specification loaded successfully");
            return openApi;
        } catch (Exception e) {
            logger.error("Error parsing OpenAPI specification", e);
            throw e;
        }
    }
}
