package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login result payload. Token cookies are set by server when MFA is not required.")
public class LoginResponse {

    @Schema(description = "Whether the user must complete MFA before receiving full session cookies", example = "false")
    private Boolean requiresMfa;

    @Schema(description = "Whether login is blocked until user confirms replacing an old active session", example = "false")
    private Boolean requiresSessionReplacement;

    @Schema(description = "Short-lived token to confirm replacing the oldest active session")
    private String sessionReplacementToken;

    @Schema(description = "Active sessions visible to user when confirmation is required")
    private List<Map<String, Object>> activeSessions;

    @Schema(description = "Deprecated: access token body field, now delivered via HttpOnly cookie")
    private String accessToken;

    @Schema(description = "Deprecated: refresh token body field, now delivered via HttpOnly cookie")
    private String refreshToken;

    @Schema(description = "Pre-authentication token used for MFA verification flow (present when MFA is required)")
    private String preAuthToken;

    @Schema(description = "MFA method expected for this login attempt", example = "TOTP")
    private String mfaType;

    @SuppressWarnings("unchecked")
    public static LoginResponse fromMap(Map<String, Object> data) {
        return new LoginResponse(
                (Boolean) data.get("requiresMfa"),
                (Boolean) data.getOrDefault("requiresSessionReplacement", false),
                (String) data.get("sessionReplacementToken"),
                (List<Map<String, Object>>) data.get("activeSessions"),
                (String) data.get("accessToken"),
                (String) data.get("refreshToken"),
                (String) data.get("preAuthToken"),
                (String) data.get("mfaType")
        );
    }
}
