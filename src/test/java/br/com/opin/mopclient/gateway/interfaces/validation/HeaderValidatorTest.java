package br.com.opin.mopclient.gateway.interfaces.validation;

import br.com.opin.mopclient.gateway.interfaces.enums.HttpMethod;
import br.com.opin.mopclient.gateway.interfaces.enums.HttpType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HeaderValidator Tests")
class HeaderValidatorTest {

    private static final String CLIENT_SS_ID = "RECEIVER A";
    private static final String SERVER_AS_ID = "TRANSMITTER B";
    private static final String HTTP_TYPE_REQUEST = "Request";
    private static final String HTTP_TYPE_RESPONSE = "Response";
    private static final String STATUS_CODE_OK = "200";

    @InjectMocks
    private HeaderValidator headerValidator;

    private static final String VALID_CORRELATION_ID = "my-correlation-id-123";
    private static final String VALID_ORIGIN = "client";

    private HeaderValidator.ValidationResult validate(
            String correlationId, String origin, String path, String operation,
            String httpType, String statusCode,
            String clientSSId, String serverASId) {
        return headerValidator.validate(
                correlationId, origin, path, operation, httpType, statusCode,
                clientSSId, serverASId);
    }

    private HeaderValidator.ValidationResult validateDefaults(
            String correlationId, String origin, String path, String operation) {
        return validate(correlationId, origin, path, operation,
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
    }

    @Test
    @DisplayName("Returns success when all headers are valid (including correlationId)")
    void shouldReturnSuccessWhenAllHeadersAreValid() {
        HeaderValidator.ValidationResult result = validateDefaults(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST");
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when correlationId is null")
    void shouldReturnErrorWhenCorrelationIdIsNull() {
        HeaderValidator.ValidationResult result = validate(
                null, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("X-Correlation-Id"));
        assertTrue(result.getErrorMessage().contains("correlationId"));
    }

    @Test
    @DisplayName("Returns error when correlationId is empty")
    void shouldReturnErrorWhenCorrelationIdIsEmpty() {
        HeaderValidator.ValidationResult result = validateDefaults("", VALID_ORIGIN, "/path", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'X-Correlation-Id' (correlationId) must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is empty")
    void shouldReturnErrorWhenOriginIsEmpty() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, "", "/path", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is null")
    void shouldReturnErrorWhenOriginIsNull() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, null, "/path", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is invalid (only client or server allowed)")
    void shouldReturnErrorWhenOriginIsInvalid() {
        HeaderValidator.ValidationResult result = validateDefaults(
                VALID_CORRELATION_ID, "INVALID_ORIGIN", "/path", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must be either 'client' or 'server'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Accepts client and server origins (case-insensitive)")
    void shouldAcceptValidOrigins() {
        String[] validOrigins = {"client", "server", "Client", "SERVER"};
        for (String origin : validOrigins) {
            HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, origin, "/path", "POST");
            assertTrue(result.isValid(), "Origin " + origin + " should be valid");
        }
    }

    @Test
    @DisplayName("Returns error when path is empty")
    void shouldReturnErrorWhenPathIsEmpty() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'path' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is empty")
    void shouldReturnErrorWhenOperationIsEmpty() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "");
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is null")
    void shouldReturnErrorWhenOperationIsNull() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "/path", null);
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is invalid")
    void shouldReturnErrorWhenOperationIsInvalid() {
        HeaderValidator.ValidationResult result = validateDefaults(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "INVALID_METHOD");
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
            HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "/path", method);
            assertTrue(result.isValid(), "Method " + method + " should be valid");
        }
    }

    @Test
    @DisplayName("Returns error when httpType is missing or blank")
    void shouldReturnErrorWhenHttpTypeIsMissing() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                null, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals(
                "Header 'httpType' must be one of the following values: Request, Response. Received: ''",
                result.getErrorMessage());

        result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "", null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals(
                "Header 'httpType' must be one of the following values: Request, Response. Received: ''",
                result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when httpType is invalid")
    void shouldReturnErrorWhenHttpTypeIsInvalid() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "INVALID", null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'httpType' must be one of the following values"));
        assertTrue(result.getErrorMessage().contains(HttpType.getValidValues()));
    }

    @Test
    @DisplayName("Accepts Request and Response httpType values (case-insensitive)")
    void shouldAcceptValidHttpTypeValues() {
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "Request", null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "request", null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "Response", STATUS_CODE_OK, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                "response", "404", CLIENT_SS_ID, SERVER_AS_ID).isValid());
    }

    @Test
    @DisplayName("Returns error when httpType is Response and statusCode is missing")
    void shouldReturnErrorWhenResponseHasNoStatusCode() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_RESPONSE, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals("Header 'statusCode' is required when 'httpType' is 'Response'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when httpType is Response and statusCode is invalid")
    void shouldReturnErrorWhenResponseStatusCodeIsInvalid() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_RESPONSE, "abc", CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'statusCode' must be a valid HTTP status code"));
    }

    @Test
    @DisplayName("Returns success when httpType is Request and statusCode is absent")
    void shouldReturnSuccessWhenRequestHasNoStatusCode() {
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
    }

    @Test
    @DisplayName("Returns error when httpType is Request and optional statusCode is invalid")
    void shouldReturnErrorWhenRequestStatusCodeIsInvalid() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, "abc", CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'statusCode' must be a valid HTTP status code"));
    }

    @Test
    @DisplayName("Returns success when clientSSId and serverASId are null or blank (optional headers)")
    void shouldReturnSuccessWhenPartyHeadersAreOptional() {
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, null, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, null).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, null, null).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, "", "").isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/path", "POST",
                HTTP_TYPE_REQUEST, null, "   ", "  ").isValid());
    }
}
