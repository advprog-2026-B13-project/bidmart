package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.MfaUseCase;
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
class MfaControllerTest {

    @Mock private MfaUseCase mfaUseCase;
    @Mock private AuthContext authContext;
    @Mock private AuthCookieService authCookieService;
    @Mock private SessionClientInfoResolver sessionClientInfoResolver;

    private MfaController controller;

    @BeforeEach
    void setUp() {
        controller = new MfaController(mfaUseCase, authContext, authCookieService, sessionClientInfoResolver);
    }

    @Test
    void setupTotp_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("secret", "secret-key");
        result.put("otpAuthUrl", "otpauth://totp/BidMart:test@example.com?secret=secret-key");
        when(mfaUseCase.setupTotp(userId)).thenReturn(result);

        var response = controller.setupTotp();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("secret-key", response.getBody().getData().getSecret());
    }

    @Test
    void setupTotp_alreadyActive_shouldReturn400() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        when(mfaUseCase.setupTotp(userId)).thenThrow(new IllegalStateException("Already active"));

        var response = controller.setupTotp();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void confirmTotp_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);

        Map<String, String> req = Map.of("code", "123456");
        var response = controller.confirmTotp(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mfaUseCase).confirmTotpSetup(userId, "123456");
    }

    @Test
    void confirmTotp_invalidCode_shouldReturn400() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalArgumentException("Invalid code")).when(mfaUseCase).confirmTotpSetup(any(), anyString());

        Map<String, String> req = Map.of("code", "wrong");
        var response = controller.confirmTotp(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void enableEmailMfa_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);

        var response = controller.enableEmailMfa();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mfaUseCase).enableEmailMfa(userId);
    }

    @Test
    void disableMfa_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);

        var response = controller.disableMfa();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mfaUseCase).disableMfa(userId);
    }

    @Test
    void requestEmailOtp_success() {
        Map<String, String> req = Map.of("preAuthToken", "pre-auth-token");

        var response = controller.requestEmailOtp(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mfaUseCase).requestEmailOtp("pre-auth-token");
    }

    @Test
    void requestEmailOtp_invalid_shouldReturn400() {
        doThrow(new IllegalArgumentException("Invalid")).when(mfaUseCase).requestEmailOtp(anyString());

        Map<String, String> req = Map.of("preAuthToken", "bad");
        var response = controller.requestEmailOtp(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void verifyMfa_success_shouldReturnLoginResponse() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "access");
        tokens.put("refreshToken", "refresh");
        tokens.put("requiresSessionReplacement", false);
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(mfaUseCase.verifyMfa(eq("pre-auth"), eq("123456"), any())).thenReturn(tokens);
        when(authCookieService.buildAuthCookies("access", "refresh"))
                .thenReturn(java.util.List.of("c1", "c2"));

        MfaVerifyRequest req = new MfaVerifyRequest();
        req.setPreAuthToken("pre-auth");
        req.setCode("123456");

        var response = controller.verifyMfa(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void verifyMfa_requiresSessionReplacement() {
        Map<String, Object> result = new HashMap<>();
        result.put("requiresSessionReplacement", true);
        result.put("sessionReplacementToken", "replace-token");
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(mfaUseCase.verifyMfa(eq("pre-auth"), eq("123456"), any())).thenReturn(result);

        MfaVerifyRequest req = new MfaVerifyRequest();
        req.setPreAuthToken("pre-auth");
        req.setCode("123456");

        var response = controller.verifyMfa(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().getRequiresSessionReplacement());
    }

    @Test
    void verifyMfa_invalid_shouldReturn400() {
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(mfaUseCase.verifyMfa(anyString(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid"));

        MfaVerifyRequest req = new MfaVerifyRequest();
        req.setPreAuthToken("bad");
        req.setCode("wrong");

        var response = controller.verifyMfa(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void verifyMfa_unknownMfaType_shouldReturn400() {
        when(sessionClientInfoResolver.resolve(any())).thenReturn(null);
        when(mfaUseCase.verifyMfa(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("Unknown MFA type"));

        MfaVerifyRequest req = new MfaVerifyRequest();
        req.setPreAuthToken("pre-auth");
        req.setCode("123456");

        var response = controller.verifyMfa(req, new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
