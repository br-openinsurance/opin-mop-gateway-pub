package br.com.opin.mopclient.gateway.infrastructure.interceptor;

import br.com.opin.mopclient.gateway.shared.util.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.HashMap;
import java.util.Map;

import static br.com.opin.mopclient.gateway.interfaces.constants.HttpHeaderConstants.CORRELATIONID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RabbitMQCorrelationIdInterceptor Tests")
class RabbitMQCorrelationIdInterceptorTest {

    private RabbitMQCorrelationIdInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RabbitMQCorrelationIdInterceptor();
        CorrelationIdContext.clear();
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    @DisplayName("Deve extrair correlationId dos headers da mensagem")
    void shouldExtractCorrelationIdFromMessageHeaders() {
        // Arrange
        Message message = createMessageWithCorrelationId("test-correlation-id-123");

        // Act
        interceptor.beforeProcessMessage(message);

        // Assert
        assertEquals("test-correlation-id-123", CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve gerar correlationId quando não presente nos headers")
    void shouldGenerateCorrelationIdWhenNotPresentInHeaders() {
        // Arrange
        Message message = createMessageWithoutCorrelationId();

        // Act
        interceptor.beforeProcessMessage(message);

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve gerar correlationId quando header está vazio")
    void shouldGenerateCorrelationIdWhenHeaderIsEmpty() {
        // Arrange
        Message message = createMessageWithCorrelationId("");

        // Act
        interceptor.beforeProcessMessage(message);

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve lidar com mensagem null")
    void shouldHandleNullMessage() {
        // Act
        interceptor.beforeProcessMessage(null);

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    @Test
    @DisplayName("Deve limpar correlationId do MDC após processamento")
    void shouldClearCorrelationIdFromMDCAfterProcessing() {
        // Arrange
        Message message = createMessageWithCorrelationId("test-id");
        interceptor.beforeProcessMessage(message);
        assertNotNull(CorrelationIdContext.getCorrelationId());

        // Act
        interceptor.afterProcessMessage();

        // Assert
        assertNull(CorrelationIdContext.getCorrelationId());
    }

    @Test
    @DisplayName("Deve lidar com mensagem sem MessageProperties")
    void shouldHandleMessageWithoutMessageProperties() {
        // Arrange
        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(null);

        // Act
        interceptor.beforeProcessMessage(message);

        // Assert
        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("mop-gateway-"));
    }

    private Message createMessageWithCorrelationId(String correlationId) {
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        Map<String, Object> headers = new HashMap<>();
        if (correlationId != null && !correlationId.isEmpty()) {
            headers.put(CORRELATIONID, correlationId);
        }
        properties.setHeaders(headers);
        when(message.getMessageProperties()).thenReturn(properties);
        return message;
    }

    private Message createMessageWithoutCorrelationId() {
        Message message = mock(Message.class);
        MessageProperties properties = new MessageProperties();
        properties.setHeaders(new HashMap<>());
        when(message.getMessageProperties()).thenReturn(properties);
        return message;
    }
}

