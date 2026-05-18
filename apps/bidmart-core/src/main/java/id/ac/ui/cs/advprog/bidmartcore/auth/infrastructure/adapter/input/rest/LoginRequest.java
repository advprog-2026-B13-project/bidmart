package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for user login")
public class LoginRequest {

    @Schema(description = "Registered email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Account password", example = "secureP@ss123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
