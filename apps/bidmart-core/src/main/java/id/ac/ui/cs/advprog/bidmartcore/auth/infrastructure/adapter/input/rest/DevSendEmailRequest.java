package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload to send a dev-only test OTP email")
public class DevSendEmailRequest {

    @Schema(description = "Recipient email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "OTP code to include in email. Uses 123456 when omitted", example = "654321")
    private String otp;

    @Schema(description = "OTP expiration in seconds. Uses 600 when omitted", example = "600")
    private Long ttlSeconds;

    @Schema(description = "Include clickable verification link in email", example = "true")
    private Boolean includeVerificationLink;
}

