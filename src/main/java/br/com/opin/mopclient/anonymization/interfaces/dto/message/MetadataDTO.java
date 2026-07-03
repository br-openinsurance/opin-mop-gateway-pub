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
@JsonPropertyOrder({"version", "environment", "module", "initiatedBy", "purpose", "timestamp", "contentType"})
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
