package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.infrastructure.adapter.RabbitMQMessagePublisher;
import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQService Tests")
class RabbitMQServiceTest {

    @Mock
    private RabbitMQMessagePublisher messagePublisher;

    @InjectMocks
    private RabbitMQMessageService rabbitMQService;

    private static final String VALID_MESSAGE = "Test message";
    private RequestHeadersDTO validHeadersDTO;

    @BeforeEach
    void setUp() {
        validHeadersDTO = RequestHeadersDTO.builder()
                .origin("origin-value")
                .destination("destination-value")
                .path("/test")
                .operation("POST")
                .certificate("cert-value")
                .userID("user123")
                .headers(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando RabbitMQMessagePublisher é null no construtor")
    void shouldThrowNullPointerExceptionWhenMessagePublisherIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            new RabbitMQMessageService(null);
        });
    }

    @Test
    @DisplayName("Deve enviar mensagem com sucesso")
    void shouldSendMessageSuccessfully() {
        // Arrange
        doNothing().when(messagePublisher).sendMessage(VALID_MESSAGE);

        // Act
        assertDoesNotThrow(() -> rabbitMQService.sendMessage(VALID_MESSAGE));

        // Assert
        verify(messagePublisher, times(1)).sendMessage(VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando mensagem é null")
    void shouldThrowIllegalArgumentExceptionWhenMessageIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rabbitMQService.sendMessage(null)
        );

        assertEquals("Message cannot be null or blank", exception.getMessage());
        verify(messagePublisher, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando mensagem está vazia")
    void shouldThrowIllegalArgumentExceptionWhenMessageIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rabbitMQService.sendMessage("")
        );

        assertEquals("Message cannot be null or blank", exception.getMessage());
        verify(messagePublisher, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando mensagem contém apenas espaços")
    void shouldThrowIllegalArgumentExceptionWhenMessageIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rabbitMQService.sendMessage("   ")
        );

        assertEquals("Message cannot be null or blank", exception.getMessage());
        verify(messagePublisher, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Deve relançar RabbitMQException quando messagePublisher lança RabbitMQException")
    void shouldRethrowRabbitMQExceptionWhenPublisherThrowsRabbitMQException() {
        // Arrange
        RabbitMQException exception = new RabbitMQException(
                VALID_MESSAGE,
                "Publisher error",
                new RuntimeException("Cause")
        );
        doThrow(exception).when(messagePublisher).sendMessage(VALID_MESSAGE);

        // Act & Assert
        RabbitMQException thrown = assertThrows(
                RabbitMQException.class,
                () -> rabbitMQService.sendMessage(VALID_MESSAGE)
        );

        assertEquals(exception, thrown);
        verify(messagePublisher, times(1)).sendMessage(VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando exceção inesperada ocorre")
    void shouldThrowRabbitMQExceptionWhenUnexpectedExceptionOccurs() {
        // Arrange
        RuntimeException exception = new RuntimeException("Unexpected error");
        doThrow(exception).when(messagePublisher).sendMessage(VALID_MESSAGE);

        // Act & Assert
        RabbitMQException thrown = assertThrows(
                RabbitMQException.class,
                () -> rabbitMQService.sendMessage(VALID_MESSAGE)
        );

        assertEquals("Unexpected error: Unexpected error", thrown.getReason());
        assertNotNull(thrown.getCause());
        verify(messagePublisher, times(1)).sendMessage(VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve enviar mensagem com headers com sucesso")
    void shouldSendMessageWithHeadersSuccessfully() {
        // Arrange
        doNothing().when(messagePublisher).sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act
        assertDoesNotThrow(() -> rabbitMQService.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO));

        // Assert
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(validHeadersDTO));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando mensagem é null ao enviar com headers")
    void shouldThrowIllegalArgumentExceptionWhenMessageIsNullWithHeaders() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rabbitMQService.sendMessageWithHead(null, validHeadersDTO)
        );

        assertEquals("Message cannot be null or blank", exception.getMessage());
        verify(messagePublisher, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando headersDTO é null")
    void shouldThrowNullPointerExceptionWhenHeadersDTOIsNull() {
        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> rabbitMQService.sendMessageWithHead(VALID_MESSAGE, null)
        );

        assertEquals("RequestHeadersDTO cannot be null", exception.getMessage());
        verify(messagePublisher, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando mensagem está vazia ao enviar com headers")
    void shouldThrowIllegalArgumentExceptionWhenMessageIsEmptyWithHeaders() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rabbitMQService.sendMessageWithHead("", validHeadersDTO)
        );

        assertEquals("Message cannot be null or blank", exception.getMessage());
        verify(messagePublisher, never()).sendMessageWithHead(anyString(), any());
    }

    @Test
    @DisplayName("Deve relançar RabbitMQException quando messagePublisher lança RabbitMQException com headers")
    void shouldRethrowRabbitMQExceptionWhenPublisherThrowsRabbitMQExceptionWithHeaders() {
        // Arrange
        RabbitMQException exception = new RabbitMQException(
                VALID_MESSAGE,
                "Publisher error with headers",
                new RuntimeException("Cause")
        );
        doThrow(exception).when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act & Assert
        RabbitMQException thrown = assertThrows(
                RabbitMQException.class,
                () -> rabbitMQService.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO)
        );

        assertEquals(exception, thrown);
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(validHeadersDTO));
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando exceção inesperada ocorre com headers")
    void shouldThrowRabbitMQExceptionWhenUnexpectedExceptionOccursWithHeaders() {
        // Arrange
        RuntimeException exception = new RuntimeException("Unexpected error with headers");
        doThrow(exception).when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act & Assert
        RabbitMQException thrown = assertThrows(
                RabbitMQException.class,
                () -> rabbitMQService.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO)
        );

        assertEquals("Unexpected error: Unexpected error with headers", thrown.getReason());
        assertNotNull(thrown.getCause());
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(validHeadersDTO));
    }

    @Test
    @DisplayName("Deve processar mensagem com headers contendo valores null")
    void shouldProcessMessageWithHeadersContainingNullValues() {
        // Arrange
        RequestHeadersDTO headersWithNulls = RequestHeadersDTO.builder()
                .origin(null)
                .destination(null)
                .path(null)
                .operation(null)
                .certificate(null)
                .userID(null)
                .headers(null)
                .build();

        doNothing().when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act
        assertDoesNotThrow(() -> 
                rabbitMQService.sendMessageWithHead(VALID_MESSAGE, headersWithNulls));

        // Assert
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(headersWithNulls));
    }
}

