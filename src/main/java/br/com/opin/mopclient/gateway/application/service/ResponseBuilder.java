package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.interfaces.dto.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for building HTTP responses.
 * <p>
 * Encapsulates the logic for creating structured success and error responses.
 */
@Service
public class ResponseBuilder {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_ERROR = "ERROR";

    /**
     * Body message returned when the payload was synchronously delivered to the MOP server.
     */
    public static final String API_SUCCESS_BODY_MESSAGE =
            "Request processed successfully. Your data has been received and forwarded to the server.";

    /**
     * Body message returned when the payload could not be delivered synchronously and was stored
     * in the client retry queue for later delivery (HTTP 202 Accepted).
     */
    public static final String API_ACCEPTED_BODY_MESSAGE =
            "Request accepted and queued for later delivery to the server (MOP unavailable).";

    private static final String SUCCESS_MESSAGE = API_SUCCESS_BODY_MESSAGE;
    /**
     * Creates a structured 200 OK response — payload was delivered to the MOP server.
     * No trace object is included in the response; trace is only in the final JSON (MessageDTO).
     *
     * @param correlationId  The correlation ID from header X-Correlation-Id (user-supplied)
     * @param timestamp       The timestamp of the request
     * @param clientSSId      The client SS (receiver) identifier
     * @param serverASId      The server AS (transmitter) identifier
     * @param path            The path of the request
     * @param operation       The operation type
     * @return ResponseEntity with status 200 and structured success body
     */
    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation) {
        return buildResponse(HttpStatus.OK, STATUS_SUCCESS, SUCCESS_MESSAGE,
                correlationId, timestamp, clientSSId, serverASId, path, operation);
    }

    /**
     * 200 OK response with a custom message (defaults to {@link #API_SUCCESS_BODY_MESSAGE}).
     */
    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation,
                                                               String message) {
        return buildResponse(HttpStatus.OK, STATUS_SUCCESS,
                message != null ? message : SUCCESS_MESSAGE,
                correlationId, timestamp, clientSSId, serverASId, path, operation);
    }

    /**
     * Creates a structured 202 Accepted response — payload was stored in the client retry queue
     * because the MOP server was unreachable (or the circuit breaker is open).
     *
     * @param correlationId  The correlation ID from header X-Correlation-Id (user-supplied)
     * @param timestamp       The timestamp of the request
     * @param clientSSId      The client SS (receiver) identifier
     * @param serverASId      The server AS (transmitter) identifier
     * @param path            The path of the request
     * @param operation       The operation type
     * @param message         Optional custom message (defaults to {@link #API_ACCEPTED_BODY_MESSAGE}).
     * @return ResponseEntity with status 202 and structured accepted body
     */
    public ResponseEntity<ApiResponseDTO> buildAcceptedResponse(String correlationId, String timestamp,
                                                                String clientSSId, String serverASId,
                                                                String path, String operation,
                                                                String message) {
        return buildResponse(HttpStatus.ACCEPTED, STATUS_ACCEPTED,
                message != null ? message : API_ACCEPTED_BODY_MESSAGE,
                correlationId, timestamp, clientSSId, serverASId, path, operation);
    }

    private ResponseEntity<ApiResponseDTO> buildResponse(HttpStatus httpStatus, String bodyStatus, String message,
                                                         String correlationId, String timestamp,
                                                         String clientSSId, String serverASId,
                                                         String path, String operation) {
        ApiResponseDTO response = ApiResponseDTO.builder()
                .status(bodyStatus)
                .message(message)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .clientSSId(clientSSId)
                .serverASId(serverASId)
                .path(path)
                .operation(operation)
                .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Creates a structured error response with detailed information.
     *
     * @param status  HTTP status code
     * @param error   Error type or code
     * @param details Detailed error message
     * @return ResponseEntity with structured error response
     */
    public ResponseEntity<ApiResponseDTO> buildErrorResponse(HttpStatus status, String error, String details) {
        ApiResponseDTO response = ApiResponseDTO.builder()
                .status(STATUS_ERROR)
                .error(error)
                .details(details)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity.status(status.value()).body(response);
    }
}

