package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Session summary payload")
public class SessionSummaryResponse {

    @Schema(description = "Session identifier")
    private String sessionId;

    @Schema(description = "Whether this session is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Session expiry timestamp in ISO-8601 format", example = "2026-03-26T12:00:00Z")
    private String expiresAt;

    @Schema(description = "Whether this session is the same one as caller's current session", example = "false")
    private Boolean isCurrent;
}

