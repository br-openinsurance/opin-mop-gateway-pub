package br.com.opin.mopclient.gateway.interfaces.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpMethod Enum Tests")
class HttpMethodTest {

    @Test
    @DisplayName("Deve conter todos os métodos HTTP esperados")
    void shouldContainAllExpectedHttpMethods() {
        // Assert
        HttpMethod[] values = HttpMethod.values();
        assertEquals(8, values.length);
        assertTrue(containsValue(values, HttpMethod.GET));
        assertTrue(containsValue(values, HttpMethod.POST));
        assertTrue(containsValue(values, HttpMethod.PUT));
        assertTrue(containsValue(values, HttpMethod.PATCH));
        assertTrue(containsValue(values, HttpMethod.DELETE));
        assertTrue(containsValue(values, HttpMethod.HEAD));
        assertTrue(containsValue(values, HttpMethod.OPTIONS));
        assertTrue(containsValue(values, HttpMethod.TRACE));
    }

    @Test
    @DisplayName("isValid deve retornar true para valores válidos em maiúsculas")
    void isValidShouldReturnTrueForValidUppercaseValues() {
        // Assert
        assertTrue(HttpMethod.isValid("GET"));
        assertTrue(HttpMethod.isValid("POST"));
        assertTrue(HttpMethod.isValid("PUT"));
        assertTrue(HttpMethod.isValid("PATCH"));
        assertTrue(HttpMethod.isValid("DELETE"));
        assertTrue(HttpMethod.isValid("HEAD"));
        assertTrue(HttpMethod.isValid("OPTIONS"));
        assertTrue(HttpMethod.isValid("TRACE"));
    }

    @Test
    @DisplayName("isValid deve retornar true para valores válidos em minúsculas (case-insensitive)")
    void isValidShouldReturnTrueForValidLowercaseValues() {
        // Assert
        assertTrue(HttpMethod.isValid("get"));
        assertTrue(HttpMethod.isValid("post"));
        assertTrue(HttpMethod.isValid("put"));
        assertTrue(HttpMethod.isValid("patch"));
        assertTrue(HttpMethod.isValid("delete"));
        assertTrue(HttpMethod.isValid("head"));
        assertTrue(HttpMethod.isValid("options"));
        assertTrue(HttpMethod.isValid("trace"));
    }

    @Test
    @DisplayName("isValid deve retornar true para valores válidos em formato misto (case-insensitive)")
    void isValidShouldReturnTrueForValidMixedCaseValues() {
        // Assert
        assertTrue(HttpMethod.isValid("Get"));
        assertTrue(HttpMethod.isValid("Post"));
        assertTrue(HttpMethod.isValid("PuT"));
        assertTrue(HttpMethod.isValid("PaTcH"));
        assertTrue(HttpMethod.isValid("DeLeTe"));
    }

    @Test
    @DisplayName("isValid deve retornar false para valores inválidos")
    void isValidShouldReturnFalseForInvalidValues() {
        // Assert
        assertFalse(HttpMethod.isValid("INVALID"));
        assertFalse(HttpMethod.isValid("GETT"));
        assertFalse(HttpMethod.isValid("POSTT"));
        assertFalse(HttpMethod.isValid(""));
        assertFalse(HttpMethod.isValid("   "));
        assertFalse(HttpMethod.isValid("GET POST"));
    }

    @Test
    @DisplayName("isValid deve retornar false para null")
    void isValidShouldReturnFalseForNull() {
        // Assert
        assertFalse(HttpMethod.isValid(null));
    }

    @Test
    @DisplayName("fromString deve retornar enum correto para valores válidos em maiúsculas")
    void fromStringShouldReturnCorrectEnumForValidUppercaseValues() {
        // Assert
        assertEquals(HttpMethod.GET, HttpMethod.fromString("GET"));
        assertEquals(HttpMethod.POST, HttpMethod.fromString("POST"));
        assertEquals(HttpMethod.PUT, HttpMethod.fromString("PUT"));
        assertEquals(HttpMethod.PATCH, HttpMethod.fromString("PATCH"));
        assertEquals(HttpMethod.DELETE, HttpMethod.fromString("DELETE"));
        assertEquals(HttpMethod.HEAD, HttpMethod.fromString("HEAD"));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.fromString("OPTIONS"));
        assertEquals(HttpMethod.TRACE, HttpMethod.fromString("TRACE"));
    }

    @Test
    @DisplayName("fromString deve retornar enum correto para valores válidos em minúsculas (case-insensitive)")
    void fromStringShouldReturnCorrectEnumForValidLowercaseValues() {
        // Assert
        assertEquals(HttpMethod.GET, HttpMethod.fromString("get"));
        assertEquals(HttpMethod.POST, HttpMethod.fromString("post"));
        assertEquals(HttpMethod.PUT, HttpMethod.fromString("put"));
        assertEquals(HttpMethod.PATCH, HttpMethod.fromString("patch"));
        assertEquals(HttpMethod.DELETE, HttpMethod.fromString("delete"));
        assertEquals(HttpMethod.HEAD, HttpMethod.fromString("head"));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.fromString("options"));
        assertEquals(HttpMethod.TRACE, HttpMethod.fromString("trace"));
    }

    @Test
    @DisplayName("fromString deve retornar null para valores inválidos")
    void fromStringShouldReturnNullForInvalidValues() {
        // Assert
        assertNull(HttpMethod.fromString("INVALID"));
        assertNull(HttpMethod.fromString("GETT"));
        assertNull(HttpMethod.fromString(""));
        assertNull(HttpMethod.fromString("   "));
    }

    @Test
    @DisplayName("fromString deve retornar null para null")
    void fromStringShouldReturnNullForNull() {
        // Assert
        assertNull(HttpMethod.fromString(null));
    }

    @Test
    @DisplayName("getValidValues deve retornar string com todos os valores separados por vírgula")
    void getValidValuesShouldReturnStringWithAllValuesSeparatedByComma() {
        // Act
        String validValues = HttpMethod.getValidValues();

        // Assert
        assertNotNull(validValues);
        assertTrue(validValues.contains("GET"));
        assertTrue(validValues.contains("POST"));
        assertTrue(validValues.contains("PUT"));
        assertTrue(validValues.contains("PATCH"));
        assertTrue(validValues.contains("DELETE"));
        assertTrue(validValues.contains("HEAD"));
        assertTrue(validValues.contains("OPTIONS"));
        assertTrue(validValues.contains("TRACE"));
        assertTrue(validValues.contains(", "));
    }

    @Test
    @DisplayName("getValidValues deve conter exatamente 8 valores")
    void getValidValuesShouldContainExactlyEightValues() {
        // Act
        String validValues = HttpMethod.getValidValues();

        // Assert
        String[] values = validValues.split(", ");
        assertEquals(8, values.length);
    }

    private boolean containsValue(HttpMethod[] values, HttpMethod method) {
        for (HttpMethod value : values) {
            if (value == method) {
                return true;
            }
        }
        return false;
    }
}

