package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login result payload. Token cookies are set by server when MFA is not required.")
public class LoginResponse {

    @Schema(description = "Whether the user must complete MFA before receiving full session cookies", example = "false")
    private Boolean requiresMfa;

    @Schema(description = "Deprecated: access token body field, now delivered via HttpOnly cookie")
    private String accessToken;

    @Schema(description = "Deprecated: refresh token body field, now delivered via HttpOnly cookie")
    private String refreshToken;

    @Schema(description = "Pre-authentication token used for MFA verification flow (present when MFA is required)")
    private String preAuthToken;

    @Schema(description = "MFA method expected for this login attempt", example = "TOTP")
    private String mfaType;

    public static LoginResponse fromMap(Map<String, Object> data) {
        return new LoginResponse(
                (Boolean) data.get("requiresMfa"),
                (String) data.get("accessToken"),
                (String) data.get("refreshToken"),
                (String) data.get("preAuthToken"),
                (String) data.get("mfaType")
        );
    }
}
