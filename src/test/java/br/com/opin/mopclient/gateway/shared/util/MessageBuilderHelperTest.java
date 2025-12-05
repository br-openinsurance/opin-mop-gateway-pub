package br.com.opin.mopclient.gateway.shared.util;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.dto.MessageHeadersDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageBuilderHelper Tests")
class MessageBuilderHelperTest {

    private static final String VALID_PAYLOAD = "{\"key\":\"value\"}";

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload é null")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MessageBuilderHelper.buildMessage(null, null)
        );
        assertEquals("Payload must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload está vazio")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MessageBuilderHelper.buildMessage("", null)
        );
        assertEquals("Payload must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload contém apenas espaços")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MessageBuilderHelper.buildMessage("   ", null)
        );
        assertEquals("Payload must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve construir mensagem com headersDTO null usando valores padrão")
    void shouldBuildMessageWithNullHeadersDTOUsingDefaults() {
        // Act
        Message message = MessageBuilderHelper.buildMessage(VALID_PAYLOAD, null);

        // Assert
        assertNotNull(message);
        assertEquals(VALID_PAYLOAD, new String(message.getBody(), StandardCharsets.UTF_8));
        MessageProperties properties = message.getMessageProperties();
        assertNotNull(properties);
        assertEquals("unknown-origin", properties.getHeaders().get(ORIGIN));
        assertEquals("unknown-destination", properties.getHeaders().get(DESTINATION));
        assertNotNull(properties.getHeaders().get(CORRELATIONID));
        assertTrue(properties.getHeaders().get(CORRELATIONID).toString().startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve construir mensagem com headersDTO completo")
    void shouldBuildMessageWithCompleteHeadersDTO() {
        // Arrange
        RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
                .origin("test-origin")
                .destination("test-destination")
                .path("/test/path")
                .operation("POST")
                .certificate("test-certificate")
                .userID("test-user")
                .correlationID("test-correlation-id")
                .timestamp("2024-01-15T10:30:00Z")
                .headers(Map.of("custom-key", "custom-value"))
                .build();

        // Act
        Message message = MessageBuilderHelper.buildMessage(VALID_PAYLOAD, headersDTO);

        // Assert
        assertNotNull(message);
        MessageProperties properties = message.getMessageProperties();
        assertEquals("test-origin", properties.getHeaders().get(ORIGIN));
        assertEquals("test-destination", properties.getHeaders().get(DESTINATION));
        assertEquals("/test/path", properties.getHeaders().get(PATH));
        assertEquals("POST", properties.getHeaders().get(OPERATION));
        assertEquals("test-certificate", properties.getHeaders().get(CERTIFCATE));
        assertEquals("test-user", properties.getHeaders().get(USERID));
        assertEquals("test-correlation-id", properties.getHeaders().get(CORRELATIONID));
        assertEquals("2024-01-15T10:30:00Z", properties.getHeaders().get(TIMESTAMP));
    }

    @Test
    @DisplayName("Deve gerar correlationId quando não fornecido")
    void shouldGenerateCorrelationIdWhenNotProvided() {
        // Arrange
        RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
                .origin("test-origin")
                .destination("test-destination")
                .path("/test")
                .operation("POST")
                .certificate("cert")
                .userID("user")
                .build();

        // Act
        Message message = MessageBuilderHelper.buildMessage(VALID_PAYLOAD, headersDTO);

        // Assert
        MessageProperties properties = message.getMessageProperties();
        String correlationId = properties.getHeaders().get(CORRELATIONID).toString();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve gerar timestamp quando não fornecido")
    void shouldGenerateTimestampWhenNotProvided() {
        // Arrange
        RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
                .origin("test-origin")
                .destination("test-destination")
                .path("/test")
                .operation("POST")
                .certificate("cert")
                .userID("user")
                .build();

        // Act
        Message message = MessageBuilderHelper.buildMessage(VALID_PAYLOAD, headersDTO);

        // Assert
        MessageProperties properties = message.getMessageProperties();
        String timestamp = properties.getHeaders().get(TIMESTAMP).toString();
        assertNotNull(timestamp);
        assertFalse(timestamp.isEmpty());
    }

    @Test
    @DisplayName("Deve usar valores padrão quando headers estão vazios")
    void shouldUseDefaultValuesWhenHeadersAreEmpty() {
        // Arrange
        RequestHeadersDTO headersDTO = RequestHeadersDTO.builder()
                .origin("")
                .destination("")
                .path("")
                .operation("")
                .certificate("")
                .userID("")
                .build();

        // Act
        Message message = MessageBuilderHelper.buildMessage(VALID_PAYLOAD, headersDTO);

        // Assert
        MessageProperties properties = message.getMessageProperties();
        assertEquals("unknown-origin", properties.getHeaders().get(ORIGIN));
        assertEquals("unknown-destination", properties.getHeaders().get(DESTINATION));
        assertEquals("unknown-path", properties.getHeaders().get(PATH));
        assertEquals("unknown-operation", properties.getHeaders().get(OPERATION));
    }

    @Test
    @DisplayName("Deve extrair headers de uma mensagem")
    void shouldExtractHeadersFromMessage() {
        // Arrange
        MessageProperties properties = new MessageProperties();
        Map<String, Object> headers = new HashMap<>();
        headers.put(ORIGIN, "extracted-origin");
        headers.put(DESTINATION, "extracted-destination");
        headers.put(PATH, "/extracted/path");
        headers.put(OPERATION, "GET");
        headers.put(CERTIFCATE, "extracted-cert");
        headers.put(USERID, "extracted-user");
        headers.put(CORRELATIONID, "extracted-correlation-id");
        headers.put(TIMESTAMP, "2024-01-15T10:30:00Z");
        properties.setHeaders(headers);
        
        Message message = org.springframework.amqp.core.MessageBuilder
                .withBody(VALID_PAYLOAD.getBytes(StandardCharsets.UTF_8))
                .andProperties(properties)
                .build();

        // Act
        MessageHeadersDTO extractedHeaders = MessageBuilderHelper.extractHeaders(message);

        // Assert
        assertEquals("extracted-origin", extractedHeaders.getOrigin());
        assertEquals("extracted-destination", extractedHeaders.getDestination());
        assertEquals("/extracted/path", extractedHeaders.getPath());
        assertEquals("GET", extractedHeaders.getOperation());
        assertEquals("extracted-cert", extractedHeaders.getCertificate());
        assertEquals("extracted-user", extractedHeaders.getUserID());
        assertEquals("extracted-correlation-id", extractedHeaders.getCorrelationId());
        assertEquals("2024-01-15T10:30:00Z", extractedHeaders.getTimestamp());
    }

    @Test
    @DisplayName("Deve usar valores padrão ao extrair headers quando não presentes")
    void shouldUseDefaultValuesWhenExtractingHeadersNotPresent() {
        // Arrange
        MessageProperties properties = new MessageProperties();
        properties.setHeaders(new HashMap<>());
        
        Message message = org.springframework.amqp.core.MessageBuilder
                .withBody(VALID_PAYLOAD.getBytes(StandardCharsets.UTF_8))
                .andProperties(properties)
                .build();

        // Act
        MessageHeadersDTO extractedHeaders = MessageBuilderHelper.extractHeaders(message);

        // Assert
        assertEquals("unknown-origin", extractedHeaders.getOrigin());
        assertEquals("unknown-destination", extractedHeaders.getDestination());
        assertEquals("unknown-path", extractedHeaders.getPath());
        assertEquals("unknown-operation", extractedHeaders.getOperation());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao extrair headers de mensagem null")
    void shouldThrowNullPointerExceptionWhenExtractingHeadersFromNullMessage() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            MessageBuilderHelper.extractHeaders(null);
        });
    }

    @Test
    @DisplayName("Deve extrair body da mensagem")
    void shouldExtractBodyFromMessage() {
        // Arrange
        String body = "{\"test\":\"data\"}";
        Message message = org.springframework.amqp.core.MessageBuilder
                .withBody(body.getBytes(StandardCharsets.UTF_8))
                .build();

        // Act
        String extractedBody = MessageBuilderHelper.getBody(message);

        // Assert
        assertEquals(body, extractedBody);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao extrair body de mensagem null")
    void shouldThrowNullPointerExceptionWhenExtractingBodyFromNullMessage() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            MessageBuilderHelper.getBody(null);
        });
    }

    @Test
    @DisplayName("Deve criar MessageHeadersDTO a partir de RequestHeadersDTO")
    void shouldCreateMessageHeadersDTOFromRequestHeadersDTO() {
        // Arrange
        RequestHeadersDTO requestHeaders = RequestHeadersDTO.builder()
                .origin("test-origin")
                .destination("test-destination")
                .path("/test")
                .operation("POST")
                .certificate("cert")
                .userID("user")
                .correlationID("correlation-id")
                .timestamp("2024-01-15T10:30:00Z")
                .headers(Map.of("key", "value"))
                .build();

        // Act
        MessageHeadersDTO messageHeaders = MessageBuilderHelper.createMessageHeadersFromDTO(requestHeaders);

        // Assert
        assertEquals("test-origin", messageHeaders.getOrigin());
        assertEquals("test-destination", messageHeaders.getDestination());
        assertEquals("/test", messageHeaders.getPath());
        assertEquals("POST", messageHeaders.getOperation());
        assertEquals("cert", messageHeaders.getCertificate());
        assertEquals("user", messageHeaders.getUserID());
        assertEquals("correlation-id", messageHeaders.getCorrelationId());
        assertEquals("2024-01-15T10:30:00Z", messageHeaders.getTimestamp());
    }

    @Test
    @DisplayName("Deve gerar correlationId rastreável")
    void shouldGenerateTraceableCorrelationId() {
        // Act
        String correlationId1 = MessageBuilderHelper.generateTraceableCorrelationId();
        String correlationId2 = MessageBuilderHelper.generateTraceableCorrelationId();

        // Assert
        assertNotNull(correlationId1);
        assertNotNull(correlationId2);
        assertTrue(correlationId1.startsWith("mop-gateway-"));
        assertTrue(correlationId2.startsWith("mop-gateway-"));
        assertNotEquals(correlationId1, correlationId2); // Devem ser únicos
    }

    @Test
    @DisplayName("Deve definir content type como JSON")
    void shouldSetContentTypeAsJson() {
        // Act
        Message message = MessageBuilderHelper.buildMessage(VALID_PAYLOAD, null);

        // Assert
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, message.getMessageProperties().getContentType());
    }
}

