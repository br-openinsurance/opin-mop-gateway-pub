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
    private static final String VALID_MOP_PATH = "/open-insurance/consents/v3/consents";

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
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST");
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when correlationId is null")
    void shouldReturnErrorWhenCorrelationIdIsNull() {
        HeaderValidator.ValidationResult result = validate(
                null, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("X-Correlation-Id"));
        assertTrue(result.getErrorMessage().contains("correlationId"));
    }

    @Test
    @DisplayName("Returns error when correlationId is empty")
    void shouldReturnErrorWhenCorrelationIdIsEmpty() {
        HeaderValidator.ValidationResult result = validateDefaults("", VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'X-Correlation-Id' (correlationId) must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is empty")
    void shouldReturnErrorWhenOriginIsEmpty() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, "", "/open-insurance/consents/v3/consents", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is null")
    void shouldReturnErrorWhenOriginIsNull() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, null, "/open-insurance/consents/v3/consents", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is invalid (only client or server allowed)")
    void shouldReturnErrorWhenOriginIsInvalid() {
        HeaderValidator.ValidationResult result = validateDefaults(
                VALID_CORRELATION_ID, "INVALID_ORIGIN", "/open-insurance/consents/v3/consents", "POST");
        assertFalse(result.isValid());
        assertEquals("Header 'origin' must be either 'client' or 'server'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Accepts client and server origins when httpType matches (case-insensitive)")
    void shouldAcceptValidOrigins() {
        assertTrue(validate(
                VALID_CORRELATION_ID, "client", "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, "Client", "/open-insurance/consents/v3/consents", "POST",
                "request", null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, "server", "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_RESPONSE, STATUS_CODE_OK, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, "SERVER", "/open-insurance/consents/v3/consents", "POST",
                "response", "201", CLIENT_SS_ID, SERVER_AS_ID).isValid());
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
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "");
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is null")
    void shouldReturnErrorWhenOperationIsNull() {
        HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", null);
        assertFalse(result.isValid());
        assertEquals("Header 'operation' must not be empty", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when operation is invalid")
    void shouldReturnErrorWhenOperationIsInvalid() {
        HeaderValidator.ValidationResult result = validateDefaults(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "INVALID_METHOD");
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
            HeaderValidator.ValidationResult result = validateDefaults(VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", method);
            assertTrue(result.isValid(), "Method " + method + " should be valid");
        }
    }

    @Test
    @DisplayName("Returns error when httpType is missing or blank")
    void shouldReturnErrorWhenHttpTypeIsMissing() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                null, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals(
                "Header 'httpType' must be one of the following values: request, response. Received: ''",
                result.getErrorMessage());

        result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                "", null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals(
                "Header 'httpType' must be one of the following values: request, response. Received: ''",
                result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when httpType is invalid")
    void shouldReturnErrorWhenHttpTypeIsInvalid() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                "INVALID", null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'httpType' must be one of the following values"));
        assertTrue(result.getErrorMessage().contains(HttpType.getValidValues()));
    }

    @Test
    @DisplayName("Accepts Request and Response httpType when consistent with origin (case-insensitive)")
    void shouldAcceptValidHttpTypeValues() {
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                "Request", null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                "request", null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, "server", "/open-insurance/consents/v3/consents", "POST",
                "Response", STATUS_CODE_OK, CLIENT_SS_ID, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, "server", "/open-insurance/consents/v3/consents", "POST",
                "response", "404", CLIENT_SS_ID, SERVER_AS_ID).isValid());
    }

    @Test
    @DisplayName("Returns error when origin is client and httpType is Response")
    void shouldReturnErrorWhenClientOriginHasResponseHttpType() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_RESPONSE, STATUS_CODE_OK, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals("Header 'httpType' must be 'request' when 'origin' is 'client'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when origin is server and httpType is Request")
    void shouldReturnErrorWhenServerOriginHasRequestHttpType() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, "server", "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals("Header 'httpType' must be 'response' when 'origin' is 'server'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when httpType is Response and statusCode is missing")
    void shouldReturnErrorWhenResponseHasNoStatusCode() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, "server", "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_RESPONSE, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertEquals("Header 'statusCode' is required when 'httpType' is 'response'", result.getErrorMessage());
    }

    @Test
    @DisplayName("Returns error when httpType is Response and statusCode is invalid")
    void shouldReturnErrorWhenResponseStatusCodeIsInvalid() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, "server", "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_RESPONSE, "abc", CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'statusCode' must be a valid HTTP status code"));
    }

    @Test
    @DisplayName("Returns success when httpType is Request and statusCode is absent")
    void shouldReturnSuccessWhenRequestHasNoStatusCode() {
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID).isValid());
    }

    @Test
    @DisplayName("Returns error when httpType is Request and optional statusCode is invalid")
    void shouldReturnErrorWhenRequestStatusCodeIsInvalid() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, "abc", CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'statusCode' must be a valid HTTP status code"));
    }

    @Test
    @DisplayName("Returns error when path is only operation segment without open-insurance prefix")
    void shouldReturnErrorWhenPathIsOnlyOperationSegment() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/consents", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("full Open Insurance path"));
        assertTrue(result.getErrorMessage().contains("/consents"));
    }

    @Test
    @DisplayName("Returns error when path embeds HTTP method")
    void shouldReturnErrorWhenPathEmbedsHttpMethod() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN,
                "POST https://api.seguro.com.br/open-insurance/consents/v3/consents",
                "POST", HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Header 'path' must not include the HTTP method"));
    }

    @Test
    @DisplayName("Returns error when path embeds HTTP method different from operation")
    void shouldReturnErrorWhenPathMethodDiffersFromOperation() {
        HeaderValidator.ValidationResult result = validate(
                VALID_CORRELATION_ID, VALID_ORIGIN,
                "POST https://api.seguro.com.br/open-insurance/consents/v3/consents",
                "GET", HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, SERVER_AS_ID);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("embeds HTTP method 'POST'"));
        assertTrue(result.getErrorMessage().contains("operation' is 'GET'"));
    }

    @Test
    @DisplayName("Returns success when clientSSId and serverASId are null or blank (optional headers)")
    void shouldReturnSuccessWhenPartyHeadersAreOptional() {
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, null, SERVER_AS_ID).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, CLIENT_SS_ID, null).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, null, null).isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, "", "").isValid());
        assertTrue(validate(
                VALID_CORRELATION_ID, VALID_ORIGIN, "/open-insurance/consents/v3/consents", "POST",
                HTTP_TYPE_REQUEST, null, "   ", "  ").isValid());
    }
}
