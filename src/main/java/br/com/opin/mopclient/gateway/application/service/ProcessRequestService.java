package br.com.opin.mopclient.gateway.application.service;

import br.com.opin.mopclient.gateway.interfaces.dto.process.ProcessRequestDTO;
import br.com.opin.mopclient.gateway.shared.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for validating and processing JSON requests.
 * <p>
 * This service handles validation of incoming JSON payloads and conversion
 * to structured DTOs, as well as generating output JSON representations.
 */
@Service
public class ProcessRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRequestService.class);
    private final ObjectMapper objectMapper;

    public ProcessRequestService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a JSON string and converts it to ProcessRequestDTO.
     *
     * @param jsonString The JSON string to validate and parse.
     * @return ProcessRequestDTO if valid, null otherwise.
     * @throws IllegalArgumentException if jsonString is null or blank.
     * @throws JsonProcessingException if JSON parsing fails.
     */
    public ProcessRequestDTO validateAndParse(String jsonString) throws JsonProcessingException {
        if (!StringUtils.hasText(jsonString)) {
            throw new IllegalArgumentException("JSON string cannot be null or blank");
        }

        LOGGER.debug("Validating and parsing JSON payload. Length: {}", jsonString.length());

        // Validate JSON format
        if (!JsonUtil.isValidJson(jsonString)) {
            LOGGER.warn("Invalid JSON format detected");
            throw new IllegalArgumentException("Invalid JSON format");
        }

        // Parse to DTO
        ProcessRequestDTO requestDTO = objectMapper.readValue(jsonString, ProcessRequestDTO.class);
        LOGGER.debug("Successfully parsed JSON to ProcessRequestDTO");

        return requestDTO;
    }

    /**
     * Converts a ProcessRequestDTO to JSON string.
     *
     * @param requestDTO The DTO to convert.
     * @return JSON string representation.
     * @throws JsonProcessingException if JSON serialization fails.
     */
    public String toJson(ProcessRequestDTO requestDTO) throws JsonProcessingException {
        if (requestDTO == null) {
            throw new IllegalArgumentException("ProcessRequestDTO cannot be null");
        }

        LOGGER.debug("Converting ProcessRequestDTO to JSON");
        String json = objectMapper.writeValueAsString(requestDTO);
        LOGGER.debug("Successfully converted ProcessRequestDTO to JSON. Length: {}", json.length());

        return json;
    }

    /**
     * Validates and processes a JSON string, returning the output JSON.
     * <p>
     * This method validates the input JSON, parses it to a DTO, and then
     * converts it back to JSON (which can be used for normalization or
     * validation purposes).
     *
     * @param inputJson The input JSON string.
     * @return Output JSON string (normalized/validated version).
     * @throws IllegalArgumentException if input is invalid.
     * @throws JsonProcessingException if JSON processing fails.
     */
    public String processAndGenerateOutput(String inputJson) throws JsonProcessingException {
        ProcessRequestDTO requestDTO = validateAndParse(inputJson);
        return toJson(requestDTO);
    }
}

