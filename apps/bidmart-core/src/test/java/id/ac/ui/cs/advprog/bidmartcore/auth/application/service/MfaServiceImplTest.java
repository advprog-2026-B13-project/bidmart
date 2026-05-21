package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.TotpCredential;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PreAuthSessionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PreAuthSessionPort.PreAuthSessionData;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.TotpCredentialRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {

    @Mock
    private TotpCredentialRepositoryPort totpRepository;

    @Mock
    private EmailOtpRepositoryPort emailOtpRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PreAuthSessionPort preAuthSessionPort;

    @Mock
    private SessionUseCase sessionUseCase;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailOtpSenderPort emailOtpSenderPort;

    private MfaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MfaServiceImpl(
                totpRepository,
                emailOtpRepository,
                userRepository,
                preAuthSessionPort,
                sessionUseCase,
                passwordEncoder,
                emailOtpSenderPort
        );
        ReflectionTestUtils.setField(service, "preAuthTtlSeconds", 300L);
    }

    @Test
    void setupTotp_whenUserMissing_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.setupTotp(userId));
    }

    @Test
    void setupTotp_whenExistingActive_shouldThrow() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        TotpCredential existing = new TotpCredential();
        existing.setActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.setupTotp(userId));
    }

    @Test
    void setupTotp_whenExistingInactive_shouldReplace() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        TotpCredential existing = new TotpCredential();
        existing.setActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        Map<String, Object> result = service.setupTotp(userId);

        assertNotNull(result.get("secret"));
        assertNotNull(result.get("otpAuthUrl"));
        verify(totpRepository).deleteByUserId(userId);
        verify(totpRepository).save(any(TotpCredential.class));
    }

    @Test
    void confirmTotpSetup_whenNoCredential_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(totpRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.confirmTotpSetup(userId, "123456"));
    }

    @Test
    void confirmTotpSetup_whenInvalidCode_shouldThrow() {
        UUID userId = UUID.randomUUID();
        TotpCredential credential = new TotpCredential();
        credential.setSecretKey("secret");
        credential.setActive(false);
        credential.setUser(new User());
        when(totpRepository.findByUserId(userId)).thenReturn(Optional.of(credential));

        assertThrows(IllegalArgumentException.class, () -> service.confirmTotpSetup(userId, "not-a-number"));
    }

    @Test
    void confirmTotpSetup_whenValid_shouldActivateAndSetDefaultMfa() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        String secret = authenticator.createCredentials().getKey();
        String code = String.valueOf(authenticator.getTotpPassword(secret));

        TotpCredential credential = new TotpCredential();
        credential.setSecretKey(secret);
        credential.setActive(false);
        credential.setUser(user);

        when(totpRepository.findByUserId(userId)).thenReturn(Optional.of(credential));

        service.confirmTotpSetup(userId, code);

        assertTrue(credential.isActive());
        assertEquals(MFAType.TOTP, user.getDefault2FAMethod());
        verify(totpRepository).save(credential);
        verify(userRepository).save(user);
    }

    @Test
    void enableEmailMfa_whenUserMissing_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.enableEmailMfa(userId));
    }

    @Test
    void disableMfa_whenUserMissing_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.disableMfa(userId));
    }

    @Test
    void disableMfa_whenValid_shouldResetAndInvalidate() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setDefault2FAMethod(MFAType.EMAIL);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.disableMfa(userId);

        assertEquals(MFAType.DISABLED, user.getDefault2FAMethod());
        verify(userRepository).save(user);
        verify(totpRepository).deleteByUserId(userId);
        verify(emailOtpRepository).invalidateAllByUserId(userId);
    }

    @Test
    void requestEmailOtp_whenInvalidPreAuth_shouldThrow() {
        when(preAuthSessionPort.get("token")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.requestEmailOtp("token"));
    }

    @Test
    void requestEmailOtp_whenValid_shouldSendEmailOtp() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        when(preAuthSessionPort.get("token")).thenReturn(Optional.of(new PreAuthSessionData(userId, "EMAIL")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        service.requestEmailOtp("token");

        verify(emailOtpRepository).invalidateAllByUserId(userId);
        ArgumentCaptor<EmailOtp> otpCaptor = ArgumentCaptor.forClass(EmailOtp.class);
        verify(emailOtpRepository).save(otpCaptor.capture());
        assertFalse(otpCaptor.getValue().isUsed());
        verify(emailOtpSenderPort).sendOtpEmail(eq("user@example.com"), anyString(), eq(300L));
    }

    @Test
    void verifyMfa_whenUnknownType_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(preAuthSessionPort.get("token")).thenReturn(Optional.of(new PreAuthSessionData(userId, "SMS")));

        assertThrows(IllegalStateException.class,
                () -> service.verifyMfa("token", "123456", SessionClientInfo.unknown()));
    }

    @Test
    void verifyMfa_whenEmailOtpValid_shouldCreateSession() {
        UUID userId = UUID.randomUUID();
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setExpiresAt(Instant.now().plusSeconds(30));
        emailOtp.setOtpHash("hash");

        when(preAuthSessionPort.get("token")).thenReturn(Optional.of(new PreAuthSessionData(userId, "EMAIL")));
        when(emailOtpRepository.findLatestActiveByUserId(userId)).thenReturn(Optional.of(emailOtp));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "access");
        tokens.put("refreshToken", "refresh");
        when(sessionUseCase.createSession(eq(userId), any(SessionClientInfo.class))).thenReturn(tokens);

        Map<String, Object> result = service.verifyMfa("token", "123456", SessionClientInfo.unknown());

        assertEquals("access", result.get("accessToken"));
        verify(preAuthSessionPort).delete("token");
    }

    @Test
    void verifyMfa_whenTotpValid_shouldCreateSession() {
        UUID userId = UUID.randomUUID();
        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        String secret = authenticator.createCredentials().getKey();
        String code = String.valueOf(authenticator.getTotpPassword(secret));

        TotpCredential credential = new TotpCredential();
        credential.setSecretKey(secret);
        credential.setActive(true);

        when(preAuthSessionPort.get("token")).thenReturn(Optional.of(new PreAuthSessionData(userId, "TOTP")));
        when(totpRepository.findByUserId(userId)).thenReturn(Optional.of(credential));

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "access");
        tokens.put("refreshToken", "refresh");
        when(sessionUseCase.createSession(eq(userId), any(SessionClientInfo.class))).thenReturn(tokens);

        Map<String, Object> result = service.verifyMfa("token", code, SessionClientInfo.unknown());

        assertEquals("refresh", result.get("refreshToken"));
        verify(preAuthSessionPort).delete("token");
    }
}

