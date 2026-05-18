package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for MFA verification during login")
public class MfaVerifyRequest {

    @Schema(description = "Pre-authentication token received from login when 2FA is required", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String preAuthToken;

    @Schema(description = "TOTP code (6 digits) or Email OTP code (6 digits)", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
