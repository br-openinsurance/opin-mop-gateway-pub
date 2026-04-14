package br.com.opin.mopclient.anonymization.interfaces.dto.message;

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
public class PrivacyDTO {

    @JsonProperty("anonymizedFields")
    private List<String> anonymizedFields;

    @JsonProperty("exposedFields")
    private List<String> exposedFields;
}
