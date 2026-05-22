package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for password reset email")
public class PasswordResetRequest {

    @Schema(description = "Registered email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}

