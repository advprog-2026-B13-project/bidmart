package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
@AllArgsConstructor
@Schema(description = "Token payload. In normal flow tokens are issued via HttpOnly cookies.")
public class TokenResponse {

    @Schema(description = "Deprecated: access token body field, now delivered via HttpOnly cookie")
    private String accessToken;

    @Schema(description = "Deprecated: refresh token body field, now delivered via HttpOnly cookie")
    private String refreshToken;

    public static TokenResponse fromMap(Map<String, Object> data) {
        return new TokenResponse(
                (String) data.get("accessToken"),
                (String) data.get("refreshToken")
        );
    }
}
