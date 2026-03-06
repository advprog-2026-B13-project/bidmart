package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for user registration")
public class RegisterRequest {

    @Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Password (min 8 characters recommended)", example = "secureP@ss123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Display name shown to other users", example = "John Doe")
    private String displayName;
}
