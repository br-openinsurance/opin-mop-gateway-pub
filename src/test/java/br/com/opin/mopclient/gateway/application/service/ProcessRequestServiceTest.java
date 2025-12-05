package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.interfaces.dto.process.ProcessRequestDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.process.ProcessingContextDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.process.RequestInfoDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.process.TraceabilityFieldsDTO;
import br.com.opin.mopclient.gateway.interfaces.dto.process.ValidationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessRequestService Tests")
class ProcessRequestServiceTest {

    private ProcessRequestService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ProcessRequestService(objectMapper);
    }

    @Test
    @DisplayName("Deve validar e parsear JSON válido corretamente")
    void shouldValidateAndParseValidJson() throws JsonProcessingException {
        // Arrange
        String validJson = """
            {
              "origin": "A",
              "destination": "C",
              "path": "",
              "headers": {
                "Content-Type": "application/json",
                "correlationId": "2025-09-30T11:25:00Z"
              },
              "request": {
                "method": "POST",
                "host": "mop-server-entrypoint-dev.intranet.opinbrasil",
                "path": "/process",
                "url": "http://mop-server-entrypoint-dev.intranet.opinbrasil/process"
              },
              "traceabilityFields": {
                "anonymized": ["cnpjNumber"],
                "exposed": ["consentId"],
                "processingContext": {
                  "module": "MOP",
                  "initiatedBy": "mop-client-gateway",
                  "purpose": "Data anonymization",
                  "environment": "dev"
                }
              },
              "data": {},
              "validations": []
            }
            """;

        // Act
        ProcessRequestDTO result = service.validateAndParse(validJson);

        // Assert
        assertNotNull(result);
        assertEquals("A", result.getOrigin());
        assertEquals("C", result.getDestination());
        assertEquals("", result.getPath());
        assertNotNull(result.getHeaders());
        assertNotNull(result.getRequest());
        assertNotNull(result.getTraceabilityFields());
        assertNotNull(result.getData());
        assertNotNull(result.getValidations());
    }

    @Test
    @DisplayName("Deve lançar exceção para JSON inválido")
    void shouldThrowExceptionForInvalidJson() {
        // Arrange
        String invalidJson = "{ invalid json }";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.validateAndParse(invalidJson);
        });
    }

    @Test
    @DisplayName("Deve lançar exceção para JSON null ou vazio")
    void shouldThrowExceptionForNullOrEmptyJson() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.validateAndParse(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            service.validateAndParse("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            service.validateAndParse("   ");
        });
    }

    @Test
    @DisplayName("Deve converter ProcessRequestDTO para JSON corretamente")
    void shouldConvertDtoToJson() throws JsonProcessingException {
        // Arrange
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("correlationId", "test-id");

        RequestInfoDTO requestInfo = RequestInfoDTO.builder()
                .method("POST")
                .host("example.com")
                .path("/process")
                .url("http://example.com/process")
                .build();

        ProcessingContextDTO processingContext = ProcessingContextDTO.builder()
                .module("MOP")
                .initiatedBy("mop-client-gateway")
                .purpose("Data anonymization")
                .environment("dev")
                .build();

        TraceabilityFieldsDTO traceabilityFields = TraceabilityFieldsDTO.builder()
                .anonymized(List.of("cnpjNumber"))
                .exposed(List.of("consentId"))
                .processingContext(processingContext)
                .build();

        ValidationDTO validation = ValidationDTO.builder()
                .violation("Test violation")
                .code(1000)
                .severity("ERROR")
                .attribute("body.data")
                .build();

        ProcessRequestDTO requestDTO = ProcessRequestDTO.builder()
                .origin("A")
                .destination("C")
                .path("")
                .headers(headers)
                .request(requestInfo)
                .traceabilityFields(traceabilityFields)
                .data(new HashMap<>())
                .validations(List.of(validation))
                .build();

        // Act
        String json = service.toJson(requestDTO);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"origin\":\"A\""));
        assertTrue(json.contains("\"destination\":\"C\""));
        assertTrue(json.contains("\"method\":\"POST\""));
    }

    @Test
    @DisplayName("Deve processar e gerar JSON de saída corretamente")
    void shouldProcessAndGenerateOutput() throws JsonProcessingException {
        // Arrange
        String inputJson = """
            {
              "origin": "A",
              "destination": "C",
              "path": "",
              "headers": {
                "Content-Type": "application/json"
              },
              "request": {
                "method": "POST",
                "host": "example.com",
                "path": "/process",
                "url": "http://example.com/process"
              },
              "traceabilityFields": {
                "anonymized": [],
                "exposed": [],
                "processingContext": {
                  "module": "MOP",
                  "initiatedBy": "mop-client-gateway",
                  "purpose": "Data anonymization",
                  "environment": "dev"
                }
              },
              "data": {},
              "validations": []
            }
            """;

        // Act
        String outputJson = service.processAndGenerateOutput(inputJson);

        // Assert
        assertNotNull(outputJson);
        assertTrue(outputJson.contains("\"origin\""));
        assertTrue(outputJson.contains("\"destination\""));
        
        // Verificar que é um JSON válido parseando novamente
        ProcessRequestDTO parsed = service.validateAndParse(outputJson);
        assertNotNull(parsed);
    }

    @Test
    @DisplayName("Deve lançar exceção ao converter DTO null para JSON")
    void shouldThrowExceptionWhenConvertingNullDtoToJson() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.toJson(null);
        });
    }
}

