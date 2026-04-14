package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import br.com.opin.mopclient.anonymization.interfaces.dto.validation.Validation;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    @JsonProperty("metadata")
    private MetadataDTO metadata;

    @JsonProperty("trace")
    private TraceDTO trace;

    @JsonProperty("request")
    private HttpRequestInfoDTO request;

    @JsonProperty("privacy")
    private PrivacyDTO privacy;

    @JsonProperty("validations")
    private List<Validation> validations;

    @JsonProperty("payload")
    private PayloadDTO payload;
}
