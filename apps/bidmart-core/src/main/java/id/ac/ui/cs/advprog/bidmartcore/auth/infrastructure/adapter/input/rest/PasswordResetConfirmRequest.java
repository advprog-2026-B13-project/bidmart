package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for completing password reset")
public class PasswordResetConfirmRequest {

    @Schema(description = "Password reset token", example = "2b32e6f2-2b56-4b38-8fb7-8c018d6efc9b", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Schema(description = "New account password", example = "NewSecureP@ss123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}

