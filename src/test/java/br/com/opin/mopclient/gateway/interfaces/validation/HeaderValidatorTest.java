package br.com.opin.mopclient.gateway.interfaces.validation;

import br.com.opin.mopclient.gateway.interfaces.enums.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HeaderValidator Tests")
class HeaderValidatorTest {

    @InjectMocks
    private HeaderValidator headerValidator;

    @Test
    @DisplayName("Deve retornar sucesso quando todos os headers são válidos")
    void shouldReturnSuccessWhenAllHeadersAreValid() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando origin está vazio")
    void shouldReturnErrorWhenOriginIsEmpty() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando origin é null")
    void shouldReturnErrorWhenOriginIsNull() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                null,
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando destination está vazio")
    void shouldReturnErrorWhenDestinationIsEmpty() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'destination' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando path está vazio")
    void shouldReturnErrorWhenPathIsEmpty() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'path' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando operation está vazio")
    void shouldReturnErrorWhenOperationIsEmpty() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando operation é null")
    void shouldReturnErrorWhenOperationIsNull() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                null,
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando operation é inválido")
    void shouldReturnErrorWhenOperationIsInvalid() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "INVALID_METHOD",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'operation' must be one of the following values"));
        assertTrue(result.getErrorMessage().contains("INVALID_METHOD"));
        assertTrue(result.getErrorMessage().contains(HttpMethod.getValidValues()));
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - GET")
    void shouldAcceptValidHttpMethodGet() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "GET",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - POST")
    void shouldAcceptValidHttpMethodPost() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - PUT")
    void shouldAcceptValidHttpMethodPut() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "PUT",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - PATCH")
    void shouldAcceptValidHttpMethodPatch() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "PATCH",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - DELETE")
    void shouldAcceptValidHttpMethodDelete() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "DELETE",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - HEAD")
    void shouldAcceptValidHttpMethodHead() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "HEAD",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - OPTIONS")
    void shouldAcceptValidHttpMethodOptions() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "OPTIONS",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar todos os métodos HTTP válidos - TRACE")
    void shouldAcceptValidHttpMethodTrace() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "TRACE",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar métodos HTTP em minúsculas (case-insensitive)")
    void shouldAcceptHttpMethodCaseInsensitive() {
        // Act
        HeaderValidator.ValidationResult resultLower = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "post",
                "user123",
                "TRANSMITTER"
        );

        HeaderValidator.ValidationResult resultMixed = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "Get",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(resultLower.isValid());
        assertTrue(resultMixed.isValid());
    }

    @Test
    @DisplayName("Deve retornar erro quando userID está vazio")
    void shouldReturnErrorWhenUserIDIsEmpty() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "",
                "TRANSMITTER"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'userID' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando applicationMode está vazio")
    void shouldReturnErrorWhenApplicationModeIsEmpty() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                ""
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'applicationMode' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve retornar erro quando applicationMode é inválido")
    void shouldReturnErrorWhenApplicationModeIsInvalid() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "INVALID_MODE"
        );

        // Assert
        assertFalse(result.isValid());
        assertEquals("Header 'applicationMode' must be either 'TRANSMITTER' or 'RECEIVER'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Deve aceitar applicationMode TRANSMITTER")
    void shouldAcceptApplicationModeTransmitter() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "TRANSMITTER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar applicationMode RECEIVER")
    void shouldAcceptApplicationModeReceiver() {
        // Act
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "RECEIVER"
        );

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Deve aceitar applicationMode case-insensitive")
    void shouldAcceptApplicationModeCaseInsensitive() {
        // Act
        HeaderValidator.ValidationResult resultLower = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "transmitter"
        );

        HeaderValidator.ValidationResult resultMixed = headerValidator.validate(
                "origin-value",
                "destination-value",
                "/path",
                "POST",
                "user123",
                "Receiver"
        );

        // Assert
        assertTrue(resultLower.isValid());
        assertTrue(resultMixed.isValid());
    }
}

