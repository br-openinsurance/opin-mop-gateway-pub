package br.com.opin.mopclient.gateway.shared.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CorrelationIdContext Tests")
class CorrelationIdContextTest {

    @BeforeEach
    void setUp() {
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    @DisplayName("Deve definir correlationId quando fornecido")
    void shouldSetCorrelationIdWhenProvided() {
        // Arrange
        String correlationId = "test-correlation-id-123";

        // Act
        CorrelationIdContext.setCorrelationId(correlationId);

        // Assert
        assertEquals(correlationId, CorrelationIdContext.getCorrelationId());
        assertEquals(correlationId, MDC.get("correlationId"));
    }

    @Test
    @DisplayName("Deve gerar correlationId quando fornecido null")
    void shouldGenerateCorrelationIdWhenProvidedNull() {
        // Act
        CorrelationIdContext.setCorrelationId(null);

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve gerar correlationId quando fornecido vazio")
    void shouldGenerateCorrelationIdWhenProvidedEmpty() {
        // Act
        CorrelationIdContext.setCorrelationId("");

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve gerar correlationId quando fornecido apenas espaços")
    void shouldGenerateCorrelationIdWhenProvidedBlank() {
        // Act
        CorrelationIdContext.setCorrelationId("   ");

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve retornar null quando correlationId não está definido")
    void shouldReturnNullWhenCorrelationIdIsNotSet() {
        // Act
        String correlationId = CorrelationIdContext.getCorrelationId();

        // Assert
        assertNull(correlationId);
    }

    @Test
    @DisplayName("Deve limpar correlationId do MDC")
    void shouldClearCorrelationIdFromMDC() {
        // Arrange
        CorrelationIdContext.setCorrelationId("test-id");

        // Act
        CorrelationIdContext.clear();

        // Assert
        assertNull(CorrelationIdContext.getCorrelationId());
        assertFalse(CorrelationIdContext.isSet());
    }

    @Test
    @DisplayName("Deve retornar false quando correlationId não está definido")
    void shouldReturnFalseWhenCorrelationIdIsNotSet() {
        // Act & Assert
        assertFalse(CorrelationIdContext.isSet());
    }

    @Test
    @DisplayName("Deve retornar true quando correlationId está definido")
    void shouldReturnTrueWhenCorrelationIdIsSet() {
        // Arrange
        CorrelationIdContext.setCorrelationId("test-id");

        // Act & Assert
        assertTrue(CorrelationIdContext.isSet());
    }

    @Test
    @DisplayName("Deve sobrescrever correlationId existente")
    void shouldOverwriteExistingCorrelationId() {
        // Arrange
        CorrelationIdContext.setCorrelationId("old-id");

        // Act
        CorrelationIdContext.setCorrelationId("new-id");

        // Assert
        assertEquals("new-id", CorrelationIdContext.getCorrelationId());
    }
}

