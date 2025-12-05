package br.com.opin.mopclient.gateway.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Custom exception to provide detailed error information when a message fails to be sent to RabbitMQ.
 */
@Getter
@AllArgsConstructor
public class RabbitMQException extends RuntimeException {

    private  String failedMessage = null; // Message that failed to send
    private  String reason = null; // Cause of failure

    public RabbitMQException(String failedMessage, String reason, Throwable cause) {
        super(reason, cause);
        this.failedMessage = failedMessage;
        this.reason = reason;
    }

    public RabbitMQException(String message) {
        super(message);
    }

}
