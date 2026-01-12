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
    private static final String SUCCESS_MESSAGE = "Request processed successfully. Your data has been received and forwarded to the queue.";

    /**
     * Creates a structured success response with detailed information.
     *
     * @param correlationId   The correlation ID for tracking
     * @param timestamp       The timestamp of the request
     * @param origin          The origin of the request
     * @param destination    The destination of the request
     * @param path            The path of the request
     * @param operation       The operation type
     * @param applicationMode The application mode
     * @return ResponseEntity with structured success response
     */
    public ResponseEntity<ApiResponseDTO> buildSuccessResponse(String correlationId, String timestamp,
                                                               String origin, String destination,
                                                               String path, String operation,
                                                               String applicationMode) {
        ApiResponseDTO response = ApiResponseDTO.builder()
                .status(STATUS_SUCCESS)
                .message(SUCCESS_MESSAGE)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .origin(origin)
                .destination(destination)
                .path(path)
                .operation(operation)
                .applicationMode(applicationMode)
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

