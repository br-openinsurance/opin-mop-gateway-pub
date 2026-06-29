package br.com.opin.mopclient.retry.domain;

/**
 * Reason why a retry message was moved to the Dead-Letter Queue.
 */
public enum ClientRetryDlqReason {
    MAX_RETRY_ATTEMPTS_EXCEEDED,
    UNPARSEABLE_MESSAGE
}
