package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
