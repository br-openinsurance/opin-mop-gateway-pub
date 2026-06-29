package br.com.opin.mopclient.retry.application;

import br.com.opin.mopclient.retry.domain.ClientRetryDlqReason;
import br.com.opin.mopclient.retry.domain.ClientRetryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Handles replay failures: re-enqueues with incremented attempts or moves to DLQ after the configured limit.
 */
@Service
public class ClientRetryFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientRetryFailureHandler.class);

    public enum FailureOutcome {
        REQUEUED_TO_RETRY,
        MOVED_TO_DLQ
    }

    private final ClientRetryQueuePublisher queuePublisher;
    private final int maxAttempts;

    public ClientRetryFailureHandler(
            ClientRetryQueuePublisher queuePublisher,
            @Value("${mop.client.retry.dlq.max-attempts:5}") int maxAttempts) {
        this.queuePublisher = Objects.requireNonNull(queuePublisher, "ClientRetryQueuePublisher cannot be null");
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    /**
     * @return {@link FailureOutcome#MOVED_TO_DLQ} when the limit was reached (message acked on retry queue)
     */
    public FailureOutcome handleReplayFailure(ClientRetryMessage message, String failureDetail) {
        Objects.requireNonNull(message, "message cannot be null");
        int nextAttempt = currentAttemptCount(message) + 1;
        message.setAttemptCount(nextAttempt);
        message.setLastFailureAt(Instant.now().toString());
        message.setLastFailureDetail(truncate(failureDetail, 4000));

        String correlationId = message.getCorrelationId() != null ? message.getCorrelationId() : "unknown";

        if (nextAttempt >= maxAttempts) {
            message.setMovedToDlqAt(Instant.now().toString());
            message.setDlqReason(ClientRetryDlqReason.MAX_RETRY_ATTEMPTS_EXCEEDED);
            queuePublisher.publishToDlq(message);
            logger.warn(
                    "[MOP retry DLQ] Event moved to DLQ after {} attempts | correlationId={} | stage={} | lastFailure={}",
                    nextAttempt,
                    correlationId,
                    message.getFailureStage(),
                    oneLine(failureDetail));
            return FailureOutcome.MOVED_TO_DLQ;
        }

        queuePublisher.publishToRetryQueue(message);
        logger.warn(
                "[MOP retry] Replay failed, re-enqueued | correlationId={} | attempt={}/{} | {}",
                correlationId,
                nextAttempt,
                maxAttempts,
                oneLine(failureDetail));
        return FailureOutcome.REQUEUED_TO_RETRY;
    }

    public void moveUnparseableToDlq(byte[] rawBody, String parseError) {
        String raw = rawBody != null ? new String(rawBody) : "";
        ClientRetryMessage message = ClientRetryMessage.builder()
                .correlationId("unknown")
                .enqueuedAt(Instant.now().toString())
                .detail("Unparseable retry queue message")
                .attemptCount(maxAttempts)
                .lastFailureAt(Instant.now().toString())
                .lastFailureDetail(truncate(parseError, 4000))
                .movedToDlqAt(Instant.now().toString())
                .dlqReason(ClientRetryDlqReason.UNPARSEABLE_MESSAGE)
                .rawQueuePayload(truncate(raw, 8000))
                .build();
        queuePublisher.publishToDlq(message);
        logger.error(
                "[MOP retry DLQ] Unparseable message moved to DLQ | parseError={} | rawPreview={}",
                oneLine(parseError),
                oneLine(raw));
    }

    int getMaxAttempts() {
        return maxAttempts;
    }

    private static int currentAttemptCount(ClientRetryMessage message) {
        return message.getAttemptCount() != null ? message.getAttemptCount() : 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String oneLine(String detail) {
        if (detail == null || detail.isBlank()) {
            return "(no detail)";
        }
        return truncate(detail.replace('\r', ' ').replace('\n', ' ').trim(), 200);
    }
}
