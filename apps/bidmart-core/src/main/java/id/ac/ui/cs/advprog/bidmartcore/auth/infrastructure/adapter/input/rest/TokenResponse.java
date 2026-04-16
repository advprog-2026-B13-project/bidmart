package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
@AllArgsConstructor
@Schema(description = "JWT token pair payload")
public class TokenResponse {

    @Schema(description = "Short-lived JWT access token")
    private String accessToken;

    @Schema(description = "Long-lived JWT refresh token")
    private String refreshToken;

    public static TokenResponse fromMap(Map<String, Object> data) {
        return new TokenResponse(
                (String) data.get("accessToken"),
                (String) data.get("refreshToken")
        );
    }
}
