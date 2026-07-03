package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"metadata", "trace", "request", "privacy", "payload"})
public class MessageDTO {

    @JsonProperty("metadata")
    private MetadataDTO metadata;

    @JsonProperty("trace")
    private TraceDTO trace;

    @JsonProperty("request")
    private HttpRequestInfoDTO request;

    @JsonProperty("privacy")
    private PrivacyDTO privacy;

    @JsonProperty("payload")
    private PayloadDTO payload;
}
