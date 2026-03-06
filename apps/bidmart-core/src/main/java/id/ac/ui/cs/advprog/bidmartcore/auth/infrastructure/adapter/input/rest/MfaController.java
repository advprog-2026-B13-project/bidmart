package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.MfaUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "Multi-Factor Authentication", description = "Setup/manage TOTP and Email OTP 2FA")
public class MfaController {

    private final MfaUseCase mfaUseCase;
    private final AuthContext authContext;

    @PostMapping("/setup-totp")
    @RequireLogin
    @Operation(
            summary = "Initiate TOTP setup",
            description = "Generates a new TOTP secret key and returns a QR-code compatible `otpauth://` URL. "
                    + "The TOTP is not active until confirmed via `/confirm-totp`."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "TOTP setup initiated — returns `secret` and `otpAuthUrl`"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "TOTP is already set up and active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> setupTotp() {
        try {
            Map<String, Object> result = mfaUseCase.setupTotp(authContext.getUserId());
            return ResponseEntity.ok(ApiResponse.success("TOTP setup initiated", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/confirm-totp")
    @RequireLogin
    @Operation(
            summary = "Confirm TOTP setup",
            description = "Verifies the 6-digit TOTP code from the authenticator app to activate TOTP 2FA on the account. "
                    + "After confirmation, the user's default 2FA method is set to TOTP."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "TOTP enabled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid TOTP code or already active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> confirmTotp(@RequestBody Map<String, String> request) {
        try {
            mfaUseCase.confirmTotpSetup(authContext.getUserId(), request.get("code"));
            return ResponseEntity.ok(ApiResponse.success("TOTP enabled", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/enable-email")
    @RequireLogin
    @Operation(
            summary = "Enable Email OTP 2FA",
            description = "Sets the user's default 2FA method to EMAIL. On next login, a 6-digit OTP will be sent to the registered email."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email MFA enabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> enableEmailMfa() {
        mfaUseCase.enableEmailMfa(authContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Email MFA enabled", null));
    }

    @PostMapping("/disable")
    @RequireLogin
    @Operation(
            summary = "Disable all 2FA",
            description = "Disables 2FA for the account, removes TOTP credentials, and invalidates existing email OTPs."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MFA disabled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> disableMfa() {
        mfaUseCase.disableMfa(authContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success("MFA disabled", null));
    }

    @PostMapping("/request-email-otp")
    @Operation(
            summary = "Request Email OTP",
            description = "Sends a 6-digit OTP to the user's registered email. Requires a valid `preAuthToken` obtained from the login endpoint. "
                    + "No Bearer token is needed — this is a pre-authentication step.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired pre-auth token")
    })
    public ResponseEntity<ApiResponse<Void>> requestEmailOtp(@RequestBody Map<String, String> request) {
        try {
            mfaUseCase.requestEmailOtp(request.get("preAuthToken"));
            return ResponseEntity.ok(ApiResponse.success("OTP sent to email", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify MFA code and complete login",
            description = "Verifies the TOTP or Email OTP code against the pre-authentication session. "
                    + "On success, the pre-auth session is consumed and a full session with `accessToken` and `refreshToken` is returned. "
                    + "No Bearer token is needed — this is a pre-authentication step.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MFA verified — returns `accessToken` and `refreshToken`"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid code, expired token, or unknown MFA type")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMfa(@RequestBody MfaVerifyRequest request) {
        try {
            Map<String, Object> result = mfaUseCase.verifyMfa(request.getPreAuthToken(), request.getCode());
            return ResponseEntity.ok(ApiResponse.success("MFA verified", result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
