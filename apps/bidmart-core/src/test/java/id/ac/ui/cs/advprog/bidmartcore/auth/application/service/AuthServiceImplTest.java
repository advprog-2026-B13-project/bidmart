package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.*;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtUtil;
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
class AuthServiceImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private RoleRepositoryPort roleRepository;

    @Mock
    private SessionRepositoryPort sessionRepository;

    @Mock
    private SessionCachePort sessionCache;

    @Mock
    private PreAuthSessionPort preAuthSession;

    @Mock
    private SessionUseCase sessionUseCase;

    @Mock
    private EmailOtpRepositoryPort emailOtpRepository;

    @Mock
    private EmailOtpSenderPort emailOtpSenderPort;

    @Mock
    private PasswordResetTokenRepositoryPort passwordResetTokenRepository;

    @Mock
    private PasswordResetEmailSenderPort passwordResetEmailSenderPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(
                userRepository,
                roleRepository,
                sessionRepository,
                sessionCache,
                preAuthSession,
                sessionUseCase,
                emailOtpRepository,
                emailOtpSenderPort,
                passwordResetTokenRepository,
                passwordResetEmailSenderPort,
                passwordEncoder,
                jwtUtil
        );
        ReflectionTestUtils.setField(service, "registrationOtpTtlSeconds", 600L);
        ReflectionTestUtils.setField(service, "verificationTokenTtlSeconds", 1800L);
        ReflectionTestUtils.setField(service, "passwordResetTokenTtlSeconds", 1800L);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(service, "preAuthTtlSeconds", 300L);
    }

    @Test
    void register_whenEmailExists_shouldThrow() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.register("existing@example.com", "Password123", "User"));
    }

    @Test
    void register_whenNewEmail_shouldCreateUserAndSendOtp() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
        Role role = new Role();
        role.setId(1);
        role.setName("USER");
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("new@example.com");
        savedUser.setDisplayName("User");
        savedUser.setRole(role);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        JwtToken verificationToken = new JwtToken();
        verificationToken.setToken("verify-token");
        verificationToken.setExpirationTime(Instant.now().plusSeconds(1800));
        when(jwtUtil.generateEmailVerificationToken(eq(savedUser.getId()), eq("new@example.com"), anyLong()))
                .thenReturn(verificationToken);

        Map<String, Object> result = service.register("new@example.com", "Password123", "User");

        assertEquals(savedUser.getId(), result.get("userId"));
        assertEquals("new@example.com", result.get("email"));
        assertEquals("User", result.get("displayName"));
        assertEquals("verify-token", result.get("verificationToken"));
        verify(emailOtpRepository).invalidateAllByUserId(savedUser.getId());
        verify(emailOtpSenderPort).sendOtpEmail(eq("new@example.com"), anyString(), eq(600L), contains("/verify-email"));
    }

    @Test
    void verifyEmailOtp_whenAlreadyActive_shouldReturn() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));

        service.verifyEmailOtp("active@example.com", "123456");

        verifyNoInteractions(emailOtpRepository);
    }

    @Test
    void verifyEmailOtp_whenSuspended_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class,
                () -> service.verifyEmailOtp("suspended@example.com", "123456"));
    }

    @Test
    void verifyEmailOtp_whenExpired_shouldThrow() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        EmailOtp otp = new EmailOtp();
        otp.setExpiresAt(Instant.now().minusSeconds(10));
        otp.setOtpHash("hash");

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        when(emailOtpRepository.findLatestActiveByUserId(user.getId())).thenReturn(Optional.of(otp));

        assertThrows(IllegalArgumentException.class,
                () -> service.verifyEmailOtp("pending@example.com", "123456"));
    }

    @Test
    void verifyEmailOtp_whenValid_shouldActivateUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        EmailOtp otp = new EmailOtp();
        otp.setExpiresAt(Instant.now().plusSeconds(30));
        otp.setOtpHash("hash");

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        when(emailOtpRepository.findLatestActiveByUserId(user.getId())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        service.verifyEmailOtp("pending@example.com", "123456");

        assertTrue(otp.isUsed());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(emailOtpRepository).save(otp);
        verify(userRepository).save(user);
    }

    @Test
    void resendVerificationOtp_whenInvalidInput_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resendVerificationOtp("", "token"));
    }

    @Test
    void resendVerificationOtp_whenActive_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> service.resendVerificationOtp("active@example.com", "token"));
    }

    @Test
    void resendVerificationOtp_whenValid_shouldSendOtp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("pending@example.com");
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        doNothing().when(jwtUtil).validateEmailVerificationToken("token", user.getId(), user.getEmail());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        service.resendVerificationOtp("pending@example.com", "token");

        verify(emailOtpRepository).invalidateAllByUserId(user.getId());
        verify(emailOtpSenderPort).sendOtpEmail(eq("pending@example.com"), anyString(), eq(600L), contains("/verify-email"));
    }

    @Test
    void login_whenMfaEnabled_shouldReturnPreAuthToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hashed");
        user.setDefault2FAMethod(id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType.EMAIL);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed")).thenReturn(true);

        Map<String, Object> result = service.login("user@example.com", "Password123", SessionClientInfo.unknown());

        assertEquals(true, result.get("requiresMfa"));
        assertEquals("EMAIL", result.get("mfaType"));
        verify(preAuthSession).save(anyString(), eq(user.getId()), eq("EMAIL"), eq(300L));
    }

    @Test
    void login_whenNoMfa_shouldReturnTokens() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hashed");
        user.setDefault2FAMethod(id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType.DISABLED);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashed")).thenReturn(true);
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "access");
        tokens.put("refreshToken", "refresh");
        when(sessionUseCase.createSession(eq(user.getId()), any(SessionClientInfo.class))).thenReturn(tokens);

        Map<String, Object> result = service.login("user@example.com", "Password123", SessionClientInfo.unknown());

        assertEquals(false, result.get("requiresMfa"));
        assertEquals("access", result.get("accessToken"));
    }

    @Test
    void confirmSessionReplacement_shouldAlwaysReturnRequiresMfaFalse() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", "access");
        tokens.put("refreshToken", "refresh");
        when(sessionUseCase.confirmSessionReplacement(eq("token"), eq(true), any(SessionClientInfo.class))).thenReturn(tokens);

        Map<String, Object> result = service.confirmSessionReplacement("token", true, SessionClientInfo.unknown());

        assertEquals(false, result.get("requiresMfa"));
    }

    @Test
    void logout_whenSessionExists_shouldDeactivateAndEvict() {
        Session session = new Session();
        session.setId("session-id");
        session.setActive(true);
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        service.logout("session-id");

        assertFalse(session.isActive());
        verify(sessionRepository).save(session);
        verify(sessionCache).evictSession("session-id");
    }

    @Test
    void refreshToken_whenValid_shouldRotateTokens() {
        Session session = new Session();
        session.setId("session-id");
        session.setActive(true);
        session.setRefreshToken("refresh-old");
        session.setExpiresAt(Instant.now().plusSeconds(300));

        when(jwtUtil.isTokenValid("refresh-old")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-old")).thenReturn(true);
        when(jwtUtil.extractSessionId("refresh-old")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.empty());
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        JwtToken newAccess = new JwtToken();
        newAccess.setToken("access-new");
        newAccess.setExpirationTime(Instant.now().plusSeconds(60));
        JwtToken newRefresh = new JwtToken();
        newRefresh.setToken("refresh-new");
        newRefresh.setExpirationTime(Instant.now().plusSeconds(3600));
        when(jwtUtil.generateAccessToken("session-id")).thenReturn(newAccess);
        when(jwtUtil.generateRefreshToken("session-id")).thenReturn(newRefresh);

        Map<String, Object> result = service.refreshToken("refresh-old");

        assertEquals("access-new", result.get("accessToken"));
        assertEquals("refresh-new", result.get("refreshToken"));
        verify(sessionRepository).save(session);
        verify(sessionCache).cacheSession(eq("session-id"), eq(session), anyLong());
    }

    @Test
    void requestPasswordReset_whenInvalidEmail_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> service.requestPasswordReset("not-an-email"));
    }

    @Test
    void requestPasswordReset_whenActiveUser_shouldSendEmail() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("active@example.com");
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("active@example.com");

        verify(passwordResetTokenRepository).invalidateAllByUserId(user.getId());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(passwordResetEmailSenderPort).sendResetEmail(eq("active@example.com"), contains("/reset-password"), eq(1800L));
    }

    @Test
    void verifyPasswordResetToken_whenInvalidToken_shouldReturnFalse() {
        assertFalse(service.verifyPasswordResetToken(""));
        assertFalse(service.verifyPasswordResetToken("not-uuid"));
    }

    @Test
    void resetPassword_whenUserSuspended_shouldThrow() {
        UUID tokenId = UUID.randomUUID();
        User user = new User();
        user.setStatus(UserStatus.SUSPENDED);
        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(60));
        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertThrows(IllegalStateException.class, () -> service.resetPassword(tokenId.toString(), "NewPass123"));
    }

    @Test
    void requestPasswordReset_whenUserNotFound_shouldNotSendEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("missing@example.com");

        verifyNoInteractions(passwordResetTokenRepository);
        verifyNoInteractions(passwordResetEmailSenderPort);
    }

    @Test
    void verifyPasswordResetToken_whenTokenValid_shouldReturnTrue() {
        UUID tokenId = UUID.randomUUID();
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);

        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(300));

        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        boolean result = service.verifyPasswordResetToken(tokenId.toString());

        assertTrue(result);
    }

    @Test
    void resetPassword_whenTokenValid_shouldUpdatePasswordAndInvalidateTokens() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);

        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(300));

        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123")).thenReturn("hashed-pass");

        service.resetPassword(tokenId.toString(), "NewPass123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hashed-pass", userCaptor.getValue().getPasswordHash());

        verify(passwordResetTokenRepository).invalidateAllByUserId(userId);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertTrue(tokenCaptor.getValue().isUsed());
    }

    @Test
    void login_whenSuspended_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class,
                () -> service.login("user@example.com", "pass", SessionClientInfo.unknown()));
    }

    @Test
    void login_whenNotVerified_shouldThrow() {
        User user = new User();
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class,
                () -> service.login("user@example.com", "pass", SessionClientInfo.unknown()));
    }

    @Test
    void login_whenWrongPassword_shouldThrow() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.login("user@example.com", "wrong", SessionClientInfo.unknown()));
    }

    @Test
    void login_whenUserNotFound_shouldThrow() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.login("missing@example.com", "pass", SessionClientInfo.unknown()));
    }

    @Test
    void logout_whenSessionNotFound_shouldDoNothing() {
        when(sessionRepository.findById("nonexistent")).thenReturn(Optional.empty());

        service.logout("nonexistent");

        verify(sessionRepository, never()).save(any());
        verify(sessionCache, never()).evictSession(anyString());
    }

    @Test
    void refreshToken_whenSessionInactive_shouldThrow() {
        Session session = new Session();
        session.setId("session-id");
        session.setActive(false);
        session.setRefreshToken("refresh");

        when(jwtUtil.isTokenValid("refresh")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh")).thenReturn(true);
        when(jwtUtil.extractSessionId("refresh")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.empty());
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken("refresh"));
    }

    @Test
    void refreshToken_whenTokenMismatch_shouldThrow() {
        Session session = new Session();
        session.setId("session-id");
        session.setActive(true);
        session.setRefreshToken("different-token");
        session.setExpiresAt(Instant.now().plusSeconds(300));

        when(jwtUtil.isTokenValid("refresh")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh")).thenReturn(true);
        when(jwtUtil.extractSessionId("refresh")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.empty());
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken("refresh"));
    }

    @Test
    void refreshToken_whenExpired_shouldThrow() {
        Session session = new Session();
        session.setId("session-id");
        session.setActive(true);
        session.setRefreshToken("refresh");
        session.setExpiresAt(Instant.now().minusSeconds(10));

        when(jwtUtil.isTokenValid("refresh")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh")).thenReturn(true);
        when(jwtUtil.extractSessionId("refresh")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.empty());
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken("refresh"));
    }

    @Test
    void refreshToken_whenInvalidJwt_shouldThrow() {
        when(jwtUtil.isTokenValid("bad")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken("bad"));
    }

    @Test
    void refreshToken_whenNotRefreshToken_shouldThrow() {
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken("token"));
    }

    @Test
    void verifyEmailOtp_whenUserNotFound_shouldThrow() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.verifyEmailOtp("missing@example.com", "123456"));
    }

    @Test
    void verifyEmailOtp_whenOtpNotFound_shouldThrow() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        when(emailOtpRepository.findLatestActiveByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.verifyEmailOtp("pending@example.com", "123456"));
    }

    @Test
    void verifyEmailOtp_whenWrongOtp_shouldThrow() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        EmailOtp otp = new EmailOtp();
        otp.setExpiresAt(Instant.now().plusSeconds(30));
        otp.setOtpHash("hash");

        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
        when(emailOtpRepository.findLatestActiveByUserId(user.getId())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.verifyEmailOtp("pending@example.com", "wrong"));
    }

    @Test
    void resendVerificationOtp_whenUserNotFound_shouldThrow() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.resendVerificationOtp("missing@example.com", "token"));
    }

    @Test
    void resetPassword_whenEmptyPassword_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(UUID.randomUUID().toString(), ""));
    }

    @Test
    void resetPassword_whenNotVerified_shouldThrow() {
        UUID tokenId = UUID.randomUUID();
        User user = new User();
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(300));
        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertThrows(IllegalStateException.class,
                () -> service.resetPassword(tokenId.toString(), "NewPass123"));
    }

    @Test
    void verifyPasswordResetToken_whenUsed_shouldReturnFalse() {
        UUID tokenId = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUsed(true);
        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertFalse(service.verifyPasswordResetToken(tokenId.toString()));
    }

    @Test
    void verifyPasswordResetToken_whenExpired_shouldReturnFalse() {
        UUID tokenId = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().minusSeconds(10));
        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertFalse(service.verifyPasswordResetToken(tokenId.toString()));
    }

    @Test
    void verifyPasswordResetToken_whenSuspendedUser_shouldReturnFalse() {
        UUID tokenId = UUID.randomUUID();
        User user = new User();
        user.setStatus(UserStatus.SUSPENDED);
        PasswordResetToken token = new PasswordResetToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(Instant.now().plusSeconds(300));
        when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        assertFalse(service.verifyPasswordResetToken(tokenId.toString()));
    }
}
