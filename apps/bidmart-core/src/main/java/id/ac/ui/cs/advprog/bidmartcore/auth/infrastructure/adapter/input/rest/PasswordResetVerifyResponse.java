package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload for password reset token validation")
public record PasswordResetVerifyResponse(
        @Schema(description = "True if the token is valid and not expired", example = "true")
        boolean valid
) {
}

