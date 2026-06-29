package br.com.opin.mopclient.retry.application;

import br.com.opin.mopclient.retry.domain.ClientRetryDlqReason;
import br.com.opin.mopclient.retry.domain.ClientRetryFailureStage;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ClientRetryFailureHandlerTest {

    @Mock
    private ClientRetryQueuePublisher queuePublisher;

    private ClientRetryFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ClientRetryFailureHandler(queuePublisher, 3);
    }

    @Test
    void shouldRequeueToRetryWhenBelowMaxAttempts() {
        ClientRetryMessage message = baseMessage();

        ClientRetryFailureHandler.FailureOutcome outcome =
                handler.handleReplayFailure(message, "MOP 503");

        assertThat(outcome).isEqualTo(ClientRetryFailureHandler.FailureOutcome.REQUEUED_TO_RETRY);
        assertThat(message.getAttemptCount()).isEqualTo(1);
        assertThat(message.getLastFailureDetail()).isEqualTo("MOP 503");
        assertThat(message.getDlqReason()).isNull();
        verify(queuePublisher).publishToRetryQueue(message);
        verifyNoMoreInteractions(queuePublisher);
    }

    @Test
    void shouldMoveToDlqWhenMaxAttemptsReached() {
        ClientRetryMessage message = baseMessage();
        message.setAttemptCount(2);

        ClientRetryFailureHandler.FailureOutcome outcome =
                handler.handleReplayFailure(message, "timeout");

        assertThat(outcome).isEqualTo(ClientRetryFailureHandler.FailureOutcome.MOVED_TO_DLQ);
        assertThat(message.getAttemptCount()).isEqualTo(3);
        assertThat(message.getDlqReason()).isEqualTo(ClientRetryDlqReason.MAX_RETRY_ATTEMPTS_EXCEEDED);
        assertThat(message.getMovedToDlqAt()).isNotBlank();
        verify(queuePublisher).publishToDlq(message);
        verifyNoMoreInteractions(queuePublisher);
    }

    @Test
    void shouldMoveUnparseablePayloadToDlq() {
        handler.moveUnparseableToDlq("not-json".getBytes(), "Unexpected token");

        ArgumentCaptor<ClientRetryMessage> captor = ArgumentCaptor.forClass(ClientRetryMessage.class);
        verify(queuePublisher).publishToDlq(captor.capture());
        ClientRetryMessage dlq = captor.getValue();
        assertThat(dlq.getDlqReason()).isEqualTo(ClientRetryDlqReason.UNPARSEABLE_MESSAGE);
        assertThat(dlq.getRawQueuePayload()).isEqualTo("not-json");
        assertThat(dlq.getLastFailureDetail()).isEqualTo("Unexpected token");
    }

    private static ClientRetryMessage baseMessage() {
        return ClientRetryMessage.builder()
                .failureStage(ClientRetryFailureStage.PROCESS_ENDPOINT)
                .correlationId("corr-1")
                .enqueuedAt("2026-01-01T00:00:00Z")
                .build();
    }
}
