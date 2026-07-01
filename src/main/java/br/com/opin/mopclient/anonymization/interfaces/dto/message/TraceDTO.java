package br.com.opin.mopclient.anonymization.interfaces.dto.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TraceDTO {

    /** Correlation ID (required). Present only in the final JSON (MessageDTO), not in the API response. */
    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("mopReportid")
    private String mopReportid;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("path")
    private String path;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("httpType")
    private String httpType;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("clientSSId")
    private String clientSSId;

    @JsonProperty("serverASId")
    private String serverASId;

    @JsonProperty("origin")
    private String origin;

    /**
     * Origin of the trace event (e.g. CLIENT, SERVER). Optional; from header {@code traceOrigin}.
     */
    @JsonProperty("traceOrigin")
    private String traceOrigin;

    @JsonProperty("OrgId")
    private String orgId;
}
