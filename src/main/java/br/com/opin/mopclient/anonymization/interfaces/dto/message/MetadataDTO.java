package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataDTO {

    @JsonProperty("version")
    private String version;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("module")
    private String module;

    @JsonProperty("initiatedBy")
    private String initiatedBy;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("contentType")
    private String contentType;
}
