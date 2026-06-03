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

    private static final String STEP = "consent-created";
    private static final String DATA_EVENTO_STEP = "2026-02-23T18:44:29.650942812Z";
    private static final String CLIENT_SS_ID = "RECEIVER A";
    private static final String SERVER_AS_ID = "TRANSMITTER B";

    @InjectMocks
    private HeaderValidator headerValidator;

    private static final String VALID_CORRELATION_ID = "my-correlation-id-123";
    private static final String VALID_ORIGIN = "client";

    @Test
    @DisplayName("Returns success when all headers are valid (including correlationId)")
    void shouldReturnSuccessWhenAllHeadersAreValid() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when correlationId is null")
    void shouldReturnErrorWhenCorrelationIdIsNull() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                null,
                VALID_ORIGIN,
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("X-Correlation-Id"));
        assertTrue(result.getErrorMessage().contains("correlationId"));
    }

    @Test
    @DisplayName("Returns error when correlationId is empty")
    void shouldReturnErrorWhenCorrelationIdIsEmpty() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                "",
                VALID_ORIGIN,
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'X-Correlation-Id' (correlationId) must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is empty")
    void shouldReturnErrorWhenOriginIsEmpty() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                "",
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is null")
    void shouldReturnErrorWhenOriginIsNull() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                null,
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is invalid (only client or server allowed)")
    void shouldReturnErrorWhenOriginIsInvalid() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                "INVALID_ORIGIN",
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must be either 'client' or 'server'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Accepts client and server origins (case-insensitive)")
    void shouldAcceptValidOrigins() {
        String[] validOrigins = {"client", "server", "Client", "SERVER"};
        for (String origin : validOrigins) {
            HeaderValidator.ValidationResult result = headerValidator.validate(
                    VALID_CORRELATION_ID, origin, "/path", "POST", STEP, DATA_EVENTO_STEP, CLIENT_SS_ID, SERVER_AS_ID);
            assertTrue(result.isValid(), "Origin " + origin + " should be valid");
        }
    }

    @Test
    @DisplayName("Returns error when path is empty")
    void shouldReturnErrorWhenPathIsEmpty() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'path' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is empty")
    void shouldReturnErrorWhenOperationIsEmpty() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "/path",
                "",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is null")
    void shouldReturnErrorWhenOperationIsNull() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "/path",
                null,
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is invalid")
    void shouldReturnErrorWhenOperationIsInvalid() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "/path",
                "INVALID_METHOD",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'operation' must be one of the following values"));
        assertTrue(result.getErrorMessage().contains("INVALID_METHOD"));
        assertTrue(result.getErrorMessage().contains(HttpMethod.getValidValues()));
    }

    @Test
    @DisplayName("Accepts all valid HTTP methods (case-insensitive)")
    void shouldAcceptAllValidHttpMethods() {
        String[] validMethods = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "get", "Post"};
        for (String method : validMethods) {
            HeaderValidator.ValidationResult result = headerValidator.validate(
                    VALID_CORRELATION_ID, VALID_ORIGIN, "/path", method, STEP, DATA_EVENTO_STEP, CLIENT_SS_ID, SERVER_AS_ID);
            assertTrue(result.isValid(), "Method " + method + " should be valid");
        }
    }

    @Test
    @DisplayName("Returns success when step and dataEventoStep are null or blank (optional headers)")
    void shouldReturnSuccessWhenStepHeadersAreOptional() {
        assertTrue(headerValidator.validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                null, null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(headerValidator.validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "", "", CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(headerValidator.validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "   ", "  ", CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(headerValidator.validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                null, DATA_EVENTO_STEP, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(headerValidator.validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                STEP, null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
    }

    @Test
    @DisplayName("Returns error when clientSSId is empty")
    void shouldReturnErrorWhenClientSSIdIsEmpty() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                "",
                SERVER_AS_ID
        );
        assertFalse(result.isValid());
        assertEquals("Header 'clientSSId' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when serverASId is empty")
    void shouldReturnErrorWhenServerASIdIsEmpty() {
        HeaderValidator.ValidationResult result = headerValidator.validate(
                VALID_CORRELATION_ID,
                VALID_ORIGIN,
                "/path",
                "POST",
                STEP,
                DATA_EVENTO_STEP,
                CLIENT_SS_ID,
                ""
        );
        assertFalse(result.isValid());
        assertEquals("Header 'serverASId' must not be empty", result.getErrorMessage());
    }
}
