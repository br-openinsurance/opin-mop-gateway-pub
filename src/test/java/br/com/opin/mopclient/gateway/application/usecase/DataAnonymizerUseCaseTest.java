package br.com.opin.mopclient.gateway.application.usecase;

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
@DisplayName("DataAnonymizerUseCase Tests")
class DataAnonymizerUseCaseTest {

    @Mock
    private RabbitMQMessagePublisher messagePublisher;

    @InjectMocks
    private DataAnonymizerUseCase dataAnonymizerUseCase;

    private static final String VALID_MESSAGE = "{\"key\":\"value\"}";
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
            new DataAnonymizerUseCase(null);
        });
    }

    @Test
    @DisplayName("Deve enviar mensagem com sucesso")
    void shouldSendMessageSuccessfully() {
        // Arrange
        doNothing().when(messagePublisher).sendMessage(VALID_MESSAGE);

        // Act
        assertDoesNotThrow(() -> dataAnonymizerUseCase.sendMessage(VALID_MESSAGE));

        // Assert
        verify(messagePublisher, times(1)).sendMessage(VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando messagePublisher lança exceção")
    void shouldThrowRabbitMQExceptionWhenPublisherThrowsException() {
        // Arrange
        RuntimeException cause = new RuntimeException("Publisher error");
        doThrow(cause).when(messagePublisher).sendMessage(VALID_MESSAGE);

        // Act & Assert
        RabbitMQException exception = assertThrows(
                RabbitMQException.class,
                () -> dataAnonymizerUseCase.sendMessage(VALID_MESSAGE)
        );

        assertEquals("Error dispatching message to RabbitMQ", exception.getReason());
        assertEquals(VALID_MESSAGE, exception.getFailedMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        verify(messagePublisher, times(1)).sendMessage(VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando messagePublisher lança RabbitMQException")
    void shouldThrowRabbitMQExceptionWhenPublisherThrowsRabbitMQException() {
        // Arrange
        RabbitMQException cause = new RabbitMQException(
                VALID_MESSAGE,
                "Publisher error",
                new RuntimeException("Root cause")
        );
        doThrow(cause).when(messagePublisher).sendMessage(VALID_MESSAGE);

        // Act & Assert
        RabbitMQException exception = assertThrows(
                RabbitMQException.class,
                () -> dataAnonymizerUseCase.sendMessage(VALID_MESSAGE)
        );

        assertEquals("Error dispatching message to RabbitMQ", exception.getReason());
        assertEquals(VALID_MESSAGE, exception.getFailedMessage());
        assertNotNull(exception.getCause());
        verify(messagePublisher, times(1)).sendMessage(VALID_MESSAGE);
    }

    @Test
    @DisplayName("Deve enviar mensagem com headers com sucesso")
    void shouldSendMessageWithHeadersSuccessfully() {
        // Arrange
        doNothing().when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act
        assertDoesNotThrow(() -> 
                dataAnonymizerUseCase.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO));

        // Assert
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(validHeadersDTO));
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando messagePublisher lança exceção com headers")
    void shouldThrowRabbitMQExceptionWhenPublisherThrowsExceptionWithHeaders() {
        // Arrange
        RuntimeException cause = new RuntimeException("Publisher error with headers");
        doThrow(cause).when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act & Assert
        RabbitMQException exception = assertThrows(
                RabbitMQException.class,
                () -> dataAnonymizerUseCase.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO)
        );

        assertEquals("Error dispatching message with headers to RabbitMQ", exception.getReason());
        assertEquals(VALID_MESSAGE, exception.getFailedMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(validHeadersDTO));
    }

    @Test
    @DisplayName("Deve lançar RabbitMQException quando messagePublisher lança RabbitMQException com headers")
    void shouldThrowRabbitMQExceptionWhenPublisherThrowsRabbitMQExceptionWithHeaders() {
        // Arrange
        RabbitMQException cause = new RabbitMQException(
                VALID_MESSAGE,
                "Publisher error with headers",
                new RuntimeException("Root cause")
        );
        doThrow(cause).when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act & Assert
        RabbitMQException exception = assertThrows(
                RabbitMQException.class,
                () -> dataAnonymizerUseCase.sendMessageWithHead(VALID_MESSAGE, validHeadersDTO)
        );

        assertEquals("Error dispatching message with headers to RabbitMQ", exception.getReason());
        assertEquals(VALID_MESSAGE, exception.getFailedMessage());
        assertNotNull(exception.getCause());
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(validHeadersDTO));
    }

    @Test
    @DisplayName("Deve processar mensagem vazia com sucesso")
    void shouldProcessEmptyMessageSuccessfully() {
        // Arrange
        String emptyMessage = "";
        doNothing().when(messagePublisher).sendMessage(emptyMessage);

        // Act
        assertDoesNotThrow(() -> dataAnonymizerUseCase.sendMessage(emptyMessage));

        // Assert
        verify(messagePublisher, times(1)).sendMessage(emptyMessage);
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
                dataAnonymizerUseCase.sendMessageWithHead(VALID_MESSAGE, headersWithNulls));

        // Assert
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(headersWithNulls));
    }

    @Test
    @DisplayName("Deve processar mensagem com headers contendo map vazio")
    void shouldProcessMessageWithHeadersContainingEmptyMap() {
        // Arrange
        RequestHeadersDTO headersWithEmptyMap = RequestHeadersDTO.builder()
                .origin("origin")
                .destination("destination")
                .path("/path")
                .operation("GET")
                .certificate("cert")
                .userID("user")
                .headers(new HashMap<>())
                .build();

        doNothing().when(messagePublisher)
                .sendMessageWithHead(anyString(), any(RequestHeadersDTO.class));

        // Act
        assertDoesNotThrow(() -> 
                dataAnonymizerUseCase.sendMessageWithHead(VALID_MESSAGE, headersWithEmptyMap));

        // Assert
        verify(messagePublisher, times(1))
                .sendMessageWithHead(eq(VALID_MESSAGE), eq(headersWithEmptyMap));
    }

    @Test
    @DisplayName("Deve processar mensagem JSON complexa com sucesso")
    void shouldProcessComplexJsonMessageSuccessfully() {
        // Arrange
        String complexJson = "{\"data\":{\"nested\":{\"value\":123}},\"array\":[1,2,3]}";
        doNothing().when(messagePublisher).sendMessage(complexJson);

        // Act
        assertDoesNotThrow(() -> dataAnonymizerUseCase.sendMessage(complexJson));

        // Assert
        verify(messagePublisher, times(1)).sendMessage(complexJson);
    }
}

