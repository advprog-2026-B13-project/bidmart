package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body to verify registration email using OTP")
public class VerifyEmailRequest {

    @Schema(description = "Registered email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "6-digit OTP code sent to email", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}

