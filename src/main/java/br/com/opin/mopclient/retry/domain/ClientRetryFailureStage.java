package br.com.opin.mopclient.retry.domain;

/**
 * Stage at which forwarding to the MOP server failed (config GET or process POST).
 */
public enum ClientRetryFailureStage {
    ANONYMIZATION_CONFIG,
    PROCESS_ENDPOINT
}
