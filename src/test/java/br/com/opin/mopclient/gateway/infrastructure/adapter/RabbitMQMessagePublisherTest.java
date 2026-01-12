package br.com.opin.mopclient.gateway.infrastructure.adapter;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQMessagePublisher Tests")
class RabbitMQMessagePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQMessagePublisher messagePublisher;

    private static final String QUEUE_NAME = "test-queue";
    private static final String VALID_MESSAGE = "Test message";
    private RequestHeadersDTO validHeadersDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(messagePublisher, "queueName", QUEUE_NAME);
        validHeadersDTO = RequestHeadersDTO.builder()
                .origin("test-origin")
                .destination("test-destination")
                .path("/test")
                .operation("POST")
                .userID("test-user")
                .applicationMode("TRANSMITTER")
                .correlationID("test-correlation-id")
                .timestamp("2024-01-15T10:00:00Z")
                .headers(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Deve enviar mensagem simples com sucesso")
    void shouldSendSimpleMessageSuccessfully() {
        // Arrange
        doNothing().when(rabbitTemplate).convertAndSend(eq(QUEUE_NAME), eq(VALID_MESSAGE));

        // Act
        assertDoesNotThrow(() -> messagePublisher.sendMessage(VALID_MESSAGE));

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(QUEUE_NAME, VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando erro ocorre ao enviar mensagem simples")
    void shouldThrowRabbitMQExceptionWhenErrorOccursSendingSimpleMessage() {
        // Arrange
        RuntimeException cause = new RuntimeException("Connection error");
        doThrow(cause).when(rabbitTemplate).convertAndSend(eq(QUEUE_NAME), eq(VALID_MESSAGE));

        // Act & Assert
        RabbitMQException exception = assertThrows(
                RabbitMQException.class,
                () -> messagePublisher.sendMessage(VALID_MESSAGE)
        );
        assertEquals("Failed to send message to RabbitMQ", exception.getReason());
        assertEquals(VALID_MESSAGE, exception.getFailedMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Deve enviar mensagem com headers com sucesso")
    void shouldSendMessageWithHeadersSuccessfully() {
        // Arrange
        doNothing().when(rabbitTemplate).send(eq(""), eq(QUEUE_NAME), any(Message.class));

        // Act
        assertDoesNotThrow(() -> messagePublisher.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO));

        // Assert
        verify(rabbitTemplate, times(1)).send(eq(""), eq(QUEUE_NAME), any(Message.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload é null ao enviar com headers")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsNullWithHeaders() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messagePublisher.sendMessageWithHead(null, validHeadersDTO)
        );
        assertEquals("Payload must not be null or blank", exception.getMessage());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando payload está vazio ao enviar com headers")
    void shouldThrowIllegalArgumentExceptionWhenPayloadIsEmptyWithHeaders() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messagePublisher.sendMessageWithHead("", validHeadersDTO)
        );
        assertEquals("Payload must not be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando erro ocorre ao enviar mensagem com headers")
    void shouldThrowRabbitMQExceptionWhenErrorOccursSendingMessageWithHeaders() {
        // Arrange
        RuntimeException cause = new RuntimeException("Connection error");
        doThrow(cause).when(rabbitTemplate).send(eq(""), eq(QUEUE_NAME), any(Message.class));

        // Act & Assert
        RabbitMQException exception = assertThrows(
                RabbitMQException.class,
                () -> messagePublisher.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO)
        );
        assertEquals("Failed to send message with headers to RabbitMQ", exception.getReason());
        assertEquals(VALID_MESSAGE, exception.getFailedMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Deve propagar IllegalArgumentException quando payload é inválido")
    void shouldPropagateIllegalArgumentExceptionWhenPayloadIsInvalid() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messagePublisher.sendMessageWithHead("   ", validHeadersDTO)
        );
        assertEquals("Payload must not be null or blank", exception.getMessage());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve construir mensagem com headers corretos")
    void shouldBuildMessageWithCorrectHeaders() {
        // Arrange
        doNothing().when(rabbitTemplate).send(eq(""), eq(QUEUE_NAME), any(Message.class));

        // Act
        messagePublisher.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO);

        // Assert
        verify(rabbitTemplate).send(
                eq(""),
                eq(QUEUE_NAME),
                argThat((Message message) -> {
                    var headers = message.getMessageProperties().getHeaders();
                    return "test-origin".equals(headers.get("origin")) &&
                           "test-destination".equals(headers.get("destination")) &&
                           "/test".equals(headers.get("path")) &&
                           "POST".equals(headers.get("operation"));
                })
        );
    }

    @Test
    @DisplayName("Deve gerar correlationId quando não fornecido nos headers")
    void shouldGenerateCorrelationIdWhenNotProvidedInHeaders() {
        // Arrange
        RequestHeadersDTO headersWithoutCorrelationId = RequestHeadersDTO.builder()
                .origin("test-origin")
                .destination("test-destination")
                .path("/test")
                .operation("POST")
                .userID("user")
                .applicationMode("TRANSMITTER")
                .headers(new HashMap<>())
                .build();
        doNothing().when(rabbitTemplate).send(eq(""), eq(QUEUE_NAME), any(Message.class));

        // Act
        messagePublisher.sendMessageWithHead(VALID_MESSAGE, headersWithoutCorrelationId);

        // Assert
        verify(rabbitTemplate).send(
                eq(""),
                eq(QUEUE_NAME),
                argThat((Message message) -> {
                    var headers = message.getMessageProperties().getHeaders();
                    String correlationId = headers.get("correlationID").toString();
                    return correlationId != null && correlationId.startsWith("mop-gateway-");
                })
        );
    }

    @Test
    @DisplayName("Deve processar mensagem mesmo quando headersDTO tem campos null")
    void shouldProcessMessageWhenHeadersDTOHasNullFields() {
        // Arrange
        RequestHeadersDTO headersWithNulls = RequestHeadersDTO.builder()
                .origin(null)
                .destination(null)
                .path(null)
                .operation(null)
                .userID(null)
                .applicationMode(null)
                .correlationID(null)
                .timestamp(null)
                .headers(null)
                .build();
        doNothing().when(rabbitTemplate).send(eq(""), eq(QUEUE_NAME), any(Message.class));

        // Act
        assertDoesNotThrow(() -> messagePublisher.sendMessageWithHead(VALID_MESSAGE, headersWithNulls));

        // Assert
        verify(rabbitTemplate, times(1)).send(eq(""), eq(QUEUE_NAME), any(Message.class));
    }

    @Test
    @DisplayName("Deve construir mensagem com todos os headers incluindo applicationMode")
    void shouldBuildMessageWithAllHeadersIncludingApplicationMode() {
        // Arrange
        doNothing().when(rabbitTemplate).send(eq(""), eq(QUEUE_NAME), any(Message.class));

        // Act
        messagePublisher.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO);

        // Assert
        verify(rabbitTemplate).send(
                eq(""),
                eq(QUEUE_NAME),
                argThat((Message message) -> {
                    var headers = message.getMessageProperties().getHeaders();
                    return "test-origin".equals(headers.get("origin")) &&
                           "test-destination".equals(headers.get("destination")) &&
                           "/test".equals(headers.get("path")) &&
                           "POST".equals(headers.get("operation")) &&
                           "test-user".equals(headers.get("userID")) &&
                           "TRANSMITTER".equals(headers.get("applicationMode")) &&
                           headers.get("correlationID") != null &&
                           headers.get("timestamp") != null;
                })
        );
    }
}

