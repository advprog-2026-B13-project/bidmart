package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AuthUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.EmailOtpSenderPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PasswordResetEmailSenderPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PasswordResetTokenRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PreAuthSessionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RoleRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthUseCase {

    private static final String REQUIRES_MFA = "requiresMfa";
    private static final org.slf4j.Logger AUDIT = org.slf4j.LoggerFactory.getLogger("id.ac.ui.cs.advprog.bidmartcore.AUDIT");

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;
    private final PreAuthSessionPort preAuthSession;
    private final SessionUseCase sessionUseCase;
    private final EmailOtpRepositoryPort emailOtpRepository;
    private final EmailOtpSenderPort emailOtpSenderPort;
    private final PasswordResetTokenRepositoryPort passwordResetTokenRepository;
    private final PasswordResetEmailSenderPort passwordResetEmailSenderPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.mfa.pre-auth-ttl-seconds:300}")
    private long preAuthTtlSeconds;

    @Value("${auth.registration.verification-otp-ttl-seconds:600}")
    private long registrationOtpTtlSeconds;

    @Value("${auth.registration.verification-token-ttl-seconds:1800}")
    private long verificationTokenTtlSeconds;

    @Value("${auth.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${auth.password-reset.token-ttl-seconds:1800}")
    private long passwordResetTokenTtlSeconds;

    @Override
    @Transactional
    public Map<String, Object> register(String email, String password, String displayName) {
        log.info("Registration attempt: email={}", email);
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed - email already exists: email={}", email);
            throw new IllegalArgumentException("Email already registered");
        }

        Role defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    return roleRepository.save(role);
                });

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setDefault2FAMethod(MFAType.DISABLED);
        user.setRole(defaultRole);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        sendEmailVerificationOtp(saved);
        AUDIT.info("USER_REGISTERED userId={} email={}", saved.getId(), saved.getEmail());
        log.info("Registration successful: userId={} email={}", saved.getId(), saved.getEmail());

        JwtToken verificationToken = jwtUtil.generateEmailVerificationToken(
                saved.getId(),
                saved.getEmail(),
                verificationTokenTtlSeconds
        );

        Map<String, Object> result = new HashMap<>();
        result.put("userId", saved.getId());
        result.put("email", saved.getEmail());
        result.put("displayName", saved.getDisplayName());
        result.put("requiresEmailVerification", true);
        result.put("verificationToken", verificationToken.getToken());
        return result;
    }

    @Override
    @Transactional
    public void verifyEmailOtp(String email, String otpCode) {
        log.info("Email verification attempt: email={}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification request"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.debug("Email already verified: email={}", email);
            return;
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Email verification rejected - account suspended: email={}", email);
            throw new IllegalStateException("Account is suspended");
        }

        EmailOtp emailOtp = emailOtpRepository.findLatestActiveByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("OTP not found or already used"));

        if (emailOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (!passwordEncoder.matches(otpCode, emailOtp.getOtpHash())) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        emailOtp.setUsed(true);
        emailOtpRepository.save(emailOtp);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        AUDIT.info("EMAIL_VERIFIED userId={} email={}", user.getId(), email);
        log.info("Email verified successfully: email={}", email);
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email, String verificationToken) {
        if (!StringUtils.hasText(email) || !email.contains("@") || !StringUtils.hasText(verificationToken)) {
            throw new IllegalArgumentException("Invalid verification request");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification request"));

        try {
            jwtUtil.validateEmailVerificationToken(verificationToken, user.getId(), user.getEmail());
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid verification request");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Account is suspended");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Email is already verified");
        }

        sendEmailVerificationOtp(user);
    }

    @Override
    @Transactional
    public Map<String, Object> login(String email, String password, SessionClientInfo clientInfo) {
        log.info("Login attempt: email={} ip={}", email, clientInfo != null ? clientInfo.ipAddress() : "unknown");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: email={}", email);
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Account is suspended");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Email is not verified");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed - invalid password: email={}", email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        AUDIT.info("LOGIN_SUCCESS userId={} email={} ip={}", user.getId(), email,
                clientInfo != null ? clientInfo.ipAddress() : "unknown");
        log.info("Login successful: userId={} email={}", user.getId(), email);

        // Check if 2FA is enabled
        if (user.getDefault2FAMethod() != MFAType.DISABLED) {
            String preAuthToken = UUID.randomUUID().toString();
            preAuthSession.save(preAuthToken, user.getId(),
                    user.getDefault2FAMethod().name(), preAuthTtlSeconds);

            Map<String, Object> result = new HashMap<>();
            result.put(REQUIRES_MFA, true);
            result.put("preAuthToken", preAuthToken);
            result.put("mfaType", user.getDefault2FAMethod().name());
            return result;
        }

        // No 2FA, create session directly
        Map<String, Object> tokens = sessionUseCase.createSession(user.getId(), clientInfo);
        tokens.put(REQUIRES_MFA, false);
        return tokens;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmSessionReplacement(String replacementToken, boolean shouldReplace, SessionClientInfo clientInfo) {
        Map<String, Object> tokens = sessionUseCase.confirmSessionReplacement(replacementToken, shouldReplace, clientInfo);
        tokens.put(REQUIRES_MFA, false);
        return tokens;
    }

    @Override
    @Transactional
    public void logout(String sessionId) {
        log.info("Logout: sessionId={}", sessionId);
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setActive(false);
            sessionRepository.save(session);
            sessionCache.evictSession(sessionId);
            AUDIT.info("LOGOUT userId={} sessionId={}", session.getUser() != null ? session.getUser().getId() : null, sessionId);
        } else {
            log.warn("Logout - session not found: sessionId={}", sessionId);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> refreshToken(String incomingRefreshToken) {
        if (!jwtUtil.isTokenValid(incomingRefreshToken) || !jwtUtil.isRefreshToken(incomingRefreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String sessionId = jwtUtil.extractSessionId(incomingRefreshToken);

        Session session = sessionCache.getCachedSession(sessionId)
                .orElseGet(() -> sessionRepository.findById(sessionId).orElse(null));

        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Session not found or inactive");
        }

        if (!session.getRefreshToken().equals(incomingRefreshToken)) {
            throw new IllegalArgumentException("Refresh token mismatch");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Session expired");
        }

        // Generate new tokens
        JwtToken newAccessToken = jwtUtil.generateAccessToken(sessionId);
        JwtToken newRefreshToken = jwtUtil.generateRefreshToken(sessionId);

        session.setRefreshToken(newRefreshToken.getToken());
        session.setExpiresAt(newRefreshToken.getExpirationTime());
        session.setLastLoginAt(Instant.now());
        sessionRepository.save(session);

        long redisTtl = newRefreshToken.getExpirationTime().toEpochMilli() - System.currentTimeMillis();
        sessionCache.cacheSession(sessionId, session, redisTtl);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken.getToken());
        result.put("refreshToken", newRefreshToken.getToken());
        return result;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested: email={}", email);
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            log.info("Password reset - user not found or inactive, silently ignoring: email={}", email);
            return;
        }

        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(passwordResetTokenTtlSeconds));
        token.setUsed(false);
        token.setCreatedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        String resetUrl = buildPasswordResetUrl(token.getId().toString());
        passwordResetEmailSenderPort.sendResetEmail(user.getEmail(), resetUrl, passwordResetTokenTtlSeconds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyPasswordResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            return false;
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findById(tokenId).orElse(null);
        if (resetToken == null || resetToken.isUsed()) {
            return false;
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }

        User user = resetToken.getUser();
        return user != null && user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset attempt: token={}", token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null");
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("New password is required");
        }

        PasswordResetToken resetToken = requireValidResetToken(token);
        User user = resetToken.getUser();

        if (user == null || user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Account is suspended");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Email is not verified");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.invalidateAllByUserId(user.getId());
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        AUDIT.info("PASSWORD_RESET userId={}", user.getId());
        log.info("Password reset successful: userId={}", user.getId());
    }

    private void sendEmailVerificationOtp(User user) {
        emailOtpRepository.invalidateAllByUserId(user.getId());

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setUser(user);
        emailOtp.setOtpHash(passwordEncoder.encode(otp));
        emailOtp.setExpiresAt(Instant.now().plusSeconds(registrationOtpTtlSeconds));
        emailOtp.setUsed(false);
        emailOtp.setCreatedAt(Instant.now());
        emailOtpRepository.save(emailOtp);

        String verificationUrl = buildVerificationUrl(user.getEmail(), otp);
        emailOtpSenderPort.sendOtpEmail(user.getEmail(), otp, registrationOtpTtlSeconds, verificationUrl);
    }

    private String buildVerificationUrl(String email, String otp) {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return null;
        }

        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedOtp = URLEncoder.encode(otp, StandardCharsets.UTF_8);
        String baseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;

        return baseUrl + "/verify-email?email=" + encodedEmail + "&otp=" + encodedOtp;
    }

    private String buildPasswordResetUrl(String token) {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return null;
        }

        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String baseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;

        return baseUrl + "/reset-password?token=" + encodedToken;
    }

    private PasswordResetToken requireValidResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        return resetToken;
    }
}
