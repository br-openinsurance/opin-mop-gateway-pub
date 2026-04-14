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
    private static final String STATUS_ERROR = "ERROR";

    /**
     * Same text returned for normal delivery and when the payload is enqueued because the MOP server is unavailable.
     */
    public static final String API_SUCCESS_BODY_MESSAGE =
            "Request processed successfully. Your data has been received and forwarded to the server.";

    private static final String SUCCESS_MESSAGE = API_SUCCESS_BODY_MESSAGE;

    /**
     * Creates a structured success response with detailed information.
     * No trace object is included in the response; trace is only in the final JSON (MessageDTO).
     *
     * @param correlationId  The correlation ID from header X-Correlation-Id (user-supplied)
     * @param timestamp       The timestamp of the request
     * @param clientSSId      The client SS (receiver) identifier
     * @param serverASId      The server AS (transmitter) identifier
     * @param path            The path of the request
     * @param operation       The operation type
     * @return ResponseEntity with structured success response
     */
    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation) {
        return buildSuccessResponse(correlationId, timestamp, clientSSId, serverASId, path, operation, SUCCESS_MESSAGE);
    }

    /**
     * Success response with a custom message (defaults to {@link #API_SUCCESS_BODY_MESSAGE}).
     */
    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String clientSSId, String serverASId,
                                                               String path, String operation,
                                                               String message) {
        ApiResponseDTO response = ApiResponseDTO.builder()
                .status(STATUS_SUCCESS)
                .message(message != null ? message : SUCCESS_MESSAGE)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .clientSSId(clientSSId)
                .serverASId(serverASId)
                .path(path)
                .operation(operation)
                .build();

        return ResponseEntity.ok(response);
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

