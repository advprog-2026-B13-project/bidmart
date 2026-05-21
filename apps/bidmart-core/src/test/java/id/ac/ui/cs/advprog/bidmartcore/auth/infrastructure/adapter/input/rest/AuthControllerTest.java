package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AuthUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthCookieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthUseCase authUseCase;
    @Mock private AuthContext authContext;
    @Mock private AuthCookieService authCookieService;
    @Mock private SessionClientInfoResolver sessionClientInfoResolver;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authUseCase, authContext, authCookieService, sessionClientInfoResolver);
    }

    private RegisterRequest registerRequest(String email, String password, String displayName) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setDisplayName(displayName);
        return req;
    }

    @Test
    void register_success_shouldReturnCreated() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("email", "test@example.com");
        result.put("displayName", "Test");
        result.put("requiresEmailVerification", true);
        result.put("verificationToken", "token");
        when(authUseCase.register("test@example.com", "Password123", "Test")).thenReturn(result);

        var response = controller.register(registerRequest("test@example.com", "Password123", "Test"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(userId, response.getBody().getData().getUserId());
    }

    @Test
    void register_emailExists_shouldReturnBadRequest() {
        when(authUseCase.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Email already registered"));

        var response = controller.register(registerRequest("existing@example.com", "pass", "name"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void register_serviceUnavailable_shouldReturn503() {
        when(authUseCase.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Email service down"));

        var response = controller.register(registerRequest("test@example.com", "pass", "name"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void verifyEmail_success_shouldReturnOk() {
        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@example.com");
        req.setCode("123456");

        var response = controller.verifyEmail(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authUseCase).verifyEmailOtp("test@example.com", "123456");
    }

    @Test
    void verifyEmail_invalidOtp_shouldReturn400() {
        doThrow(new IllegalArgumentException("Invalid OTP")).when(authUseCase).verifyEmailOtp(anyString(), anyString());

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@example.com");
        req.setCode("wrong");
        var response = controller.verifyEmail(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void verifyEmail_suspended_shouldReturn403() {
        doThrow(new IllegalStateException("Account is suspended")).when(authUseCase).verifyEmailOtp(anyString(), anyString());

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@example.com");
        req.setCode("123456");
        var response = controller.verifyEmail(req);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void resendVerificationOtp_success() {
        ResendVerificationOtpRequest req = new ResendVerificationOtpRequest();
        req.setEmail("test@example.com");
        req.setVerificationToken("token");

        var response = controller.resendVerificationOtp(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void resendVerificationOtp_suspended_shouldReturn403() {
        doThrow(new IllegalStateException("Account is suspended"))
                .when(authUseCase).resendVerificationOtp(anyString(), anyString());

        ResendVerificationOtpRequest req = new ResendVerificationOtpRequest();
        req.setEmail("test@example.com");
        req.setVerificationToken("token");
        var response = controller.resendVerificationOtp(req);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void resendVerificationOtp_serviceUnavailable() {
        doThrow(new IllegalStateException("Email service unavailable"))
                .when(authUseCase).resendVerificationOtp(anyString(), anyString());

        ResendVerificationOtpRequest req = new ResendVerificationOtpRequest();
        req.setEmail("test@example.com");
        req.setVerificationToken("token");
        var response = controller.resendVerificationOtp(req);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void resendVerificationOtp_invalidInput_shouldReturn400() {
        doThrow(new IllegalArgumentException("Invalid"))
                .when(authUseCase).resendVerificationOtp(anyString(), anyString());

        ResendVerificationOtpRequest req = new ResendVerificationOtpRequest();
        req.setEmail("test@example.com");
        req.setVerificationToken("token");
        var response = controller.resendVerificationOtp(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_success_noMfa_shouldReturnOkWithCookies() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("requiresMfa", false);
        tokens.put("requiresSessionReplacement", false);
        tokens.put("accessToken", "access-token");
        tokens.put("refreshToken", "refresh-token");
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(authUseCase.login(eq("test@example.com"), eq("pass"), any())).thenReturn(tokens);
        when(authCookieService.buildAuthCookies("access-token", "refresh-token"))
                .thenReturn(java.util.List.of("cookie1", "cookie2"));

        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass");

        var response = controller.login(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void login_mfaRequired_shouldReturnMfaResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("requiresMfa", true);
        result.put("preAuthToken", "pre-auth");
        result.put("mfaType", "EMAIL");
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(authUseCase.login(eq("test@example.com"), eq("pass"), any())).thenReturn(result);

        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass");

        var response = controller.login(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().getRequiresMfa());
    }

    @Test
    void login_invalidCredentials_shouldReturn401() {
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(authUseCase.login(anyString(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid"));

        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");

        var response = controller.login(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void login_suspended_shouldReturn403() {
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(authUseCase.login(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("Suspended"));

        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass");

        var response = controller.login(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void confirmSessionReplacement_success() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "access-token");
        tokens.put("refreshToken", "refresh-token");
        tokens.put("requiresMfa", false);
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(authUseCase.confirmSessionReplacement(eq("token"), eq(true), any())).thenReturn(tokens);
        when(authCookieService.buildAuthCookies("access-token", "refresh-token"))
                .thenReturn(java.util.List.of("cookie1", "cookie2"));

        SessionReplacementConfirmationRequest req = new SessionReplacementConfirmationRequest();
        req.setSessionReplacementToken("token");
        req.setReplaceOldestSession(true);

        var response = controller.confirmSessionReplacement(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void confirmSessionReplacement_invalid_shouldReturn400() {
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(authUseCase.confirmSessionReplacement(anyString(), anyBoolean(), any()))
                .thenThrow(new IllegalArgumentException("Invalid"));

        SessionReplacementConfirmationRequest req = new SessionReplacementConfirmationRequest();
        req.setSessionReplacementToken("token");
        req.setReplaceOldestSession(true);

        var response = controller.confirmSessionReplacement(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void logout_success() {
        when(authContext.getSessionId()).thenReturn("session-id");
        when(authCookieService.clearAuthCookies()).thenReturn(java.util.List.of("clear1", "clear2"));

        var response = controller.logout();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authUseCase).logout("session-id");
    }

    @Test
    void refresh_withCookieToken_shouldReturnNewTokens() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "new-access");
        tokens.put("refreshToken", "new-refresh");
        when(authCookieService.resolveRefreshToken(any())).thenReturn(java.util.Optional.of("refresh-token"));
        when(authUseCase.refreshToken("refresh-token")).thenReturn(tokens);
        when(authCookieService.buildAuthCookies("new-access", "new-refresh"))
                .thenReturn(java.util.List.of("c1", "c2"));

        var response = controller.refresh(null, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void refresh_withBodyToken_shouldReturnNewTokens() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "new-access");
        tokens.put("refreshToken", "new-refresh");
        when(authCookieService.resolveRefreshToken(any())).thenReturn(java.util.Optional.empty());
        when(authUseCase.refreshToken("body-refresh")).thenReturn(tokens);
        when(authCookieService.buildAuthCookies("new-access", "new-refresh"))
                .thenReturn(java.util.List.of("c1", "c2"));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("body-refresh");

        var response = controller.refresh(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void refresh_noToken_shouldReturn401() {
        when(authCookieService.resolveRefreshToken(any())).thenReturn(java.util.Optional.empty());

        var response = controller.refresh(null, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void refresh_invalidToken_shouldReturn401() {
        when(authCookieService.resolveRefreshToken(any())).thenReturn(java.util.Optional.of("bad"));
        when(authUseCase.refreshToken("bad")).thenThrow(new IllegalArgumentException("Invalid"));

        var response = controller.refresh(null, new MockHttpServletRequest());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void requestPasswordReset_success() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("test@example.com");

        var response = controller.requestPasswordReset(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void requestPasswordReset_invalidEmail_shouldReturn400() {
        doThrow(new IllegalArgumentException("Invalid")).when(authUseCase).requestPasswordReset(anyString());

        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("not-email");
        var response = controller.requestPasswordReset(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void requestPasswordReset_serviceUnavailable() {
        doThrow(new IllegalStateException("Service down")).when(authUseCase).requestPasswordReset(anyString());

        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("test@example.com");
        var response = controller.requestPasswordReset(req);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void verifyPasswordResetToken_valid() {
        when(authUseCase.verifyPasswordResetToken("token")).thenReturn(true);

        PasswordResetVerifyRequest req = new PasswordResetVerifyRequest();
        req.setToken("token");
        var response = controller.verifyPasswordResetToken(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().valid());
    }

    @Test
    void confirmPasswordReset_success() {
        PasswordResetConfirmRequest req = new PasswordResetConfirmRequest();
        req.setToken("token");
        req.setNewPassword("NewPass123");

        var response = controller.confirmPasswordReset(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authUseCase).resetPassword("token", "NewPass123");
    }

    @Test
    void confirmPasswordReset_invalid_shouldReturn400() {
        doThrow(new IllegalArgumentException("Invalid")).when(authUseCase).resetPassword(anyString(), anyString());

        PasswordResetConfirmRequest req = new PasswordResetConfirmRequest();
        req.setToken("token");
        req.setNewPassword("pass");
        var response = controller.confirmPasswordReset(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void confirmPasswordReset_suspended_shouldReturn403() {
        doThrow(new IllegalStateException("Suspended")).when(authUseCase).resetPassword(anyString(), anyString());

        PasswordResetConfirmRequest req = new PasswordResetConfirmRequest();
        req.setToken("token");
        req.setNewPassword("pass");
        var response = controller.confirmPasswordReset(req);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
