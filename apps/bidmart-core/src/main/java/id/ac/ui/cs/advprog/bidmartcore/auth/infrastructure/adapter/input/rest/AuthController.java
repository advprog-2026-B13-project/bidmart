package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AuthUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthCookieService;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Logout, Token Refresh")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final AuthContext authContext;
    private final AuthCookieService authCookieService;
    private final SessionClientInfoResolver sessionClientInfoResolver;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user account",
            description = "Creates a new user with email, password, and optional display name. Sends an OTP email for account verification before first login.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful, verification OTP sent"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email already registered or invalid input")
    })
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        try {
            Map<String, Object> result = authUseCase.register(
                    request.getEmail(), request.getPassword(), request.getDisplayName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registration successful, please verify your email", RegisterResponse.fromMap(result)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify account email with OTP",
            description = "Activates a newly registered account by validating the OTP code sent to the user's email.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid verification request or OTP"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account suspended")
    })
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        try {
            authUseCase.verifyEmailOtp(request.getEmail(), request.getCode());
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resend-verification-otp")
    @Operation(
            summary = "Resend account verification OTP",
            description = "Resends a fresh OTP code to the registered email for accounts that are not verified yet. Requires the verification token returned during registration.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification OTP resent"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email, token, or account already verified"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account suspended"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Email service unavailable")
    })
    public ResponseEntity<ApiResponse<Void>> resendVerificationOtp(@RequestBody ResendVerificationOtpRequest request) {
        try {
            authUseCase.resendVerificationOtp(request.getEmail(), request.getVerificationToken());
            return ResponseEntity.ok(ApiResponse.success("Verification OTP resent", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("suspended")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login with email and password",
            description = """
                    Authenticates the user. Three possible outcomes:
                    - **2FA disabled + session available**: authentication cookies are set directly (HttpOnly)
                    - **2FA enabled**: returns `preAuthToken` and `mfaType` - use `/api/auth/mfa/verify` to complete login
                    - **Session limit reached + confirmation strategy**: returns `sessionReplacementToken` and `activeSessions`
                    Accounts with unverified email cannot login.
                    """,
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful, 2FA required, or session replacement required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is suspended")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = authUseCase.login(
                    request.getEmail(),
                    request.getPassword(),
                    sessionClientInfoResolver.resolve(httpRequest)
            );

            if (Boolean.FALSE.equals(result.get("requiresMfa"))
                    && Boolean.FALSE.equals(result.getOrDefault("requiresSessionReplacement", false))) {
                String accessToken = (String) result.get("accessToken");
                String refreshToken = (String) result.get("refreshToken");

                ResponseEntity.BodyBuilder builder = addCookies(
                        ResponseEntity.ok(),
                        authCookieService.buildAuthCookies(accessToken, refreshToken)
                );
                return builder.body(ApiResponse.success("Login successful", LoginResponse.fromMap(result)));
            }

            return ResponseEntity.ok(ApiResponse.success("Login requires additional step", LoginResponse.fromMap(result)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/confirm-session-replacement")
    @Operation(
            summary = "Confirm replacing oldest active session",
            description = "Completes login when session limit strategy is confirmation-based. If confirmed, oldest active session is revoked and new cookies are issued.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Confirmation processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid/expired token or user canceled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> confirmSessionReplacement(
            @RequestBody SessionReplacementConfirmationRequest request,
            HttpServletRequest httpRequest) {
        try {
            boolean shouldReplace = Boolean.TRUE.equals(request.getReplaceOldestSession());
            Map<String, Object> result = authUseCase.confirmSessionReplacement(
                    request.getSessionReplacementToken(),
                    shouldReplace,
                    sessionClientInfoResolver.resolve(httpRequest)
            );

            String accessToken = (String) result.get("accessToken");
            String refreshToken = (String) result.get("refreshToken");

            ResponseEntity.BodyBuilder builder = addCookies(
                    ResponseEntity.ok(),
                    authCookieService.buildAuthCookies(accessToken, refreshToken)
            );
            return builder.body(ApiResponse.success("Login successful", LoginResponse.fromMap(result)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @RequireLogin
    @Operation(
            summary = "Logout current session",
            description = "Invalidates the current session and removes it from the Redis cache. Requires a valid access token."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        authUseCase.logout(authContext.getSessionId());
        ResponseEntity.BodyBuilder builder = addCookies(ResponseEntity.ok(), authCookieService.clearAuthCookies());
        return builder.body(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new token pair. Refresh token is read from HttpOnly cookie by default; request-body refresh token is accepted as a compatibility fallback.",
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid, expired, or mismatched refresh token")
    })
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                                              HttpServletRequest httpRequest) {
        try {
            String refreshToken = authCookieService.resolveRefreshToken(httpRequest)
                    .orElseGet(() -> request != null ? request.getRefreshToken() : null);

            if (!StringUtils.hasText(refreshToken)) {
                throw new IllegalArgumentException("Refresh token is required");
            }

            Map<String, Object> result = authUseCase.refreshToken(refreshToken);
            String accessToken = (String) result.get("accessToken");
            String newRefreshToken = (String) result.get("refreshToken");

            ResponseEntity.BodyBuilder builder = addCookies(
                    ResponseEntity.ok(),
                    authCookieService.buildAuthCookies(accessToken, newRefreshToken)
            );
            return builder.body(ApiResponse.success("Token refreshed", TokenResponse.fromMap(result)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        }
    }

    private ResponseEntity.BodyBuilder addCookies(ResponseEntity.BodyBuilder builder, Iterable<String> cookies) {
        for (String cookie : cookies) {
            builder.header(HttpHeaders.SET_COOKIE, cookie);
        }
        return builder;
    }
}
