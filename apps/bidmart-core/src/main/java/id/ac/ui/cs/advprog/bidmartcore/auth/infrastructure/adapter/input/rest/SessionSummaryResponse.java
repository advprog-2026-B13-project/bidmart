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

    @Schema(description = "Session creation timestamp in ISO-8601 format", example = "2026-03-26T10:00:00Z")
    private String createdAt;

    @Schema(description = "Last login/refresh timestamp in ISO-8601 format", example = "2026-03-26T12:00:00Z")
    private String lastLoginAt;

    @Schema(description = "Session expiry timestamp in ISO-8601 format", example = "2026-03-26T12:30:00Z")
    private String expiresAt;

    @Schema(description = "Detected client device", example = "Mozilla/5.0 (X11; Linux x86_64)")
    private String deviceInfo;

    @Schema(description = "Detected client IP address", example = "203.0.113.5")
    private String ipAddress;

    @Schema(description = "Best-effort location label", example = "ID")
    private String locationLabel;

    @Schema(description = "Whether this session is the same one as caller's current session", example = "false")
    private Boolean isCurrent;
}
