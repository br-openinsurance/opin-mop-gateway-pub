package br.com.opin.mopclient.gateway.infrastructure.adapter;

import br.com.opin.mopclient.gateway.application.service.ExternalApiClient;
import br.com.opin.mopclient.gateway.infrastructure.interceptor.RabbitMQCorrelationIdInterceptor;
import br.com.opin.mopclient.gateway.shared.exception.RabbitMQException;
import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitListener Tests")
class RabbitListenerTest {

    @Mock
    private ExternalApiClient apiClient;

    @Mock
    private RabbitMQCorrelationIdInterceptor correlationIdInterceptor;

    @InjectMocks
    private RabbitListener rabbitListener;

    private static final String QUEUE_NAME = "test-queue";
    private static final String VALID_MESSAGE_BODY = "{\"key\":\"value\"}";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rabbitListener, "queueName", QUEUE_NAME);
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
        ExecutorService executorService = (ExecutorService) ReflectionTestUtils.getField(rabbitListener, "executorService");
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Test
    @DisplayName("Deve processar mensagem com sucesso")
    void shouldProcessMessageSuccessfully() throws Exception {
        // Arrange
        Message message = createMessage(VALID_MESSAGE_BODY, "test-correlation-id");
        doNothing().when(apiClient).sendJsonPayload(VALID_MESSAGE_BODY);
        doNothing().when(correlationIdInterceptor).beforeProcessMessage(any(Message.class));
        doNothing().when(correlationIdInterceptor).afterProcessMessage();

        // Act
        rabbitListener.receiveMessage(message);

        // Assert - Aguardar um pouco para o executor processar
        Thread.sleep(100);
        verify(correlationIdInterceptor).beforeProcessMessage(message);
        verify(apiClient).sendJsonPayload(VALID_MESSAGE_BODY);
        verify(correlationIdInterceptor).afterProcessMessage();
    }

    @Test
    @DisplayName("Deve ignorar mensagem null")
    void shouldIgnoreNullMessage() {
        // Act
        assertDoesNotThrow(() -> rabbitListener.receiveMessage(null));

        // Assert - Aguardar um pouco
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        verify(apiClient, never()).sendJsonPayload(anyString());
    }

    @Test
    @DisplayName("Deve ignorar mensagem com body null")
    void shouldIgnoreMessageWithNullBody() {
        // Arrange
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(null);

        // Act
        assertDoesNotThrow(() -> rabbitListener.receiveMessage(message));

        // Assert - Aguardar um pouco
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        verify(apiClient, never()).sendJsonPayload(anyString());
    }

    @Test
    @DisplayName("Deve ignorar mensagem com body vazio")
    void shouldIgnoreMessageWithEmptyBody() {
        // Arrange
        Message message = createMessage("", "test-id");

        // Act
        assertDoesNotThrow(() -> rabbitListener.receiveMessage(message));

        // Assert - Aguardar um pouco
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        verify(apiClient, never()).sendJsonPayload(anyString());
    }

    @Test
    @DisplayName("Deve propagar correlationId do header da mensagem")
    void shouldPropagateCorrelationIdFromMessageHeader() throws Exception {
        // Arrange
        String correlationId = "test-correlation-id-123";
        Message message = createMessage(VALID_MESSAGE_BODY, correlationId);
        doNothing().when(apiClient).sendJsonPayload(VALID_MESSAGE_BODY);
        doNothing().when(correlationIdInterceptor).beforeProcessMessage(any(Message.class));
        doNothing().when(correlationIdInterceptor).afterProcessMessage();

        // Act
        rabbitListener.receiveMessage(message);

        // Assert
        Thread.sleep(100);
        verify(correlationIdInterceptor).beforeProcessMessage(message);
    }

    @Test
    @DisplayName("Deve tratar RabbitMQException durante processamento")
    void shouldHandleRabbitMQExceptionDuringProcessing() throws Exception {
        // Arrange
        Message message = createMessage(VALID_MESSAGE_BODY, "test-id");
        RabbitMQException exception = new RabbitMQException(
                VALID_MESSAGE_BODY,
                "API error",
                new RuntimeException("Cause")
        );
        doThrow(exception).when(apiClient).sendJsonPayload(VALID_MESSAGE_BODY);
        doNothing().when(correlationIdInterceptor).beforeProcessMessage(any(Message.class));
        doNothing().when(correlationIdInterceptor).afterProcessMessage();

        // Act
        assertDoesNotThrow(() -> rabbitListener.receiveMessage(message));

        // Assert
        Thread.sleep(100);
        verify(correlationIdInterceptor).afterProcessMessage();
    }

    @Test
    @DisplayName("Deve tratar exceção genérica durante processamento")
    void shouldHandleGenericExceptionDuringProcessing() throws Exception {
        // Arrange
        Message message = createMessage(VALID_MESSAGE_BODY, "test-id");
        RuntimeException exception = new RuntimeException("Unexpected error");
        doThrow(exception).when(apiClient).sendJsonPayload(VALID_MESSAGE_BODY);
        doNothing().when(correlationIdInterceptor).beforeProcessMessage(any(Message.class));
        doNothing().when(correlationIdInterceptor).afterProcessMessage();

        // Act
        assertDoesNotThrow(() -> rabbitListener.receiveMessage(message));

        // Assert
        Thread.sleep(100);
        verify(correlationIdInterceptor).afterProcessMessage();
    }

    private Message createMessage(String body, String correlationId) {
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        Map<String, Object> headers = new HashMap<>();
        if (correlationId != null) {
            headers.put(CORRELATIONID, correlationId);
        }
        properties.setHeaders(headers);
        
        lenient().when(message.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        lenient().when(message.getMessageProperties()).thenReturn(properties);
        
        return message;
    }
}

