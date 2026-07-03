package br.com.opin.mopclient.validator.application.service;

import br.com.opin.mopclient.validator.shared.util.FileUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Schema;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenApiSpecCompatibilityPatcher")
class OpenApiSpecCompatibilityPatcherTest {

    @Test
    @DisplayName("removes numeric format from string shareholding in customers spec")
    void removesNumericFormatFromStringShareholding() throws Exception {
        OpenApi3 openApi = parseCustomersSpec();
        Schema shareholdingBefore = shareholdingSchema(openApi);
        assertNotNull(shareholdingBefore);
        assertEquals("double", shareholdingBefore.getFormat());

        OpenApiSpecCompatibilityPatcher.patch(openApi);

        Schema shareholdingAfter = shareholdingSchema(openApi);
        assertNotNull(shareholdingAfter);
        assertNull(shareholdingAfter.getFormat());
        assertEquals("string", shareholdingAfter.getType());
        assertNotNull(shareholdingAfter.getPattern());
    }

    private static OpenApi3 parseCustomersSpec() throws Exception {
        try (InputStream inputStream = new ClassPathResource("swagger/current/customers.yaml").getInputStream()) {
            return new OpenApi3Parser().parse(FileUtils.inputStreamToFile(inputStream, "customers.yaml"), false);
        }
    }

    private static Schema shareholdingSchema(OpenApi3 openApi) {
        Schema businessParties = openApi.getComponents().getSchema("BusinessParties");
        assertNotNull(businessParties);
        Schema item = businessParties.getItemsSchema();
        assertNotNull(item);
        return item.getProperty("shareholding");
    }
}
