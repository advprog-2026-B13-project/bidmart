package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for token refresh. Optional fallback when refresh cookie is unavailable.")
public class RefreshTokenRequest {

    @Schema(description = "Optional fallback refresh token when cookie is not sent", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String refreshToken;
}
