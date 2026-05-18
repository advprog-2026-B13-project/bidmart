package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TOTP setup payload")
public class TotpSetupResponse {

    @Schema(description = "Generated TOTP secret key", example = "JBSWY3DPEHPK3PXP")
    private String secret;

    @Schema(description = "otpauth URL for QR generation and authenticator app enrollment")
    private String otpAuthUrl;

    public static TotpSetupResponse fromMap(Map<String, Object> data) {
        return new TotpSetupResponse(
                (String) data.get("secret"),
                (String) data.get("otpAuthUrl")
        );
    }
}

