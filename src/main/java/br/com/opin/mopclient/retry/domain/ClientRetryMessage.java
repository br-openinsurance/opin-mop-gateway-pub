package br.com.opin.mopclient.retry.domain;

import br.com.opin.mopclient.gateway.interfaces.dto.RequestHeadersDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload published to {@code mop.client.retry.queue} for later reprocessing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientRetryMessage {

    private ClientRetryFailureStage failureStage;
    private String detail;
    private String correlationId;
    private String enqueuedAt;
    /**
     * Exact HTTP request body as received by the gateway (before parse/canonicalization).
     * Use this to replay the client payload byte-for-byte when needed.
     */
    private String originalRequestBody;
    /** Normalized JSON after gateway parse (same string used for validation/anonymization). */
    private String requestPayload;
    /** Snapshot of structured headers plus the raw header map at enqueue time. */
    private RequestHeadersDTO headers;
    /** Present when failure happened after the message was fully built for /process. */
    private String wrappedPayloadForProcess;
    /** Number of failed replay attempts (0 when first enqueued). */
    private Integer attemptCount;
    private String lastFailureAt;
    private String lastFailureDetail;
    private String movedToDlqAt;
    private ClientRetryDlqReason dlqReason;
    /** Raw queue body when the message could not be parsed (DLQ traceability). */
    private String rawQueuePayload;
}
