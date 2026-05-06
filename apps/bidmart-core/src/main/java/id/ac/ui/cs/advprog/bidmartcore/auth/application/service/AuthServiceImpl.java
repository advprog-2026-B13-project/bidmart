package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AuthUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.*;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.JwtException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;
    private final PreAuthSessionPort preAuthSession;
    private final SessionUseCase sessionUseCase;
    private final EmailOtpRepositoryPort emailOtpRepository;
    private final EmailOtpSenderPort emailOtpSenderPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.mfa.pre-auth-ttl-seconds:300}")
    private long preAuthTtlSeconds;

    @Value("${auth.registration.verification-otp-ttl-seconds:600}")
    private long registrationOtpTtlSeconds;

    @Value("${auth.registration.verification-token-ttl-seconds:1800}")
    private long verificationTokenTtlSeconds;

    @Value("${auth.registration.verification-frontend-base-url:http://localhost:3000}")
    private String verificationFrontendBaseUrl;

    @Override
    @Transactional
    public Map<String, Object> register(String email, String password, String displayName) {
        if (userRepository.existsByEmail(email)) {
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification request"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Account is suspended");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Email is not verified");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Check if 2FA is enabled
        if (user.getDefault2FAMethod() != MFAType.DISABLED) {
            String preAuthToken = UUID.randomUUID().toString();
            preAuthSession.save(preAuthToken, user.getId(),
                    user.getDefault2FAMethod().name(), preAuthTtlSeconds);

            Map<String, Object> result = new HashMap<>();
            result.put("requiresMfa", true);
            result.put("preAuthToken", preAuthToken);
            result.put("mfaType", user.getDefault2FAMethod().name());
            return result;
        }

        // No 2FA, create session directly
        Map<String, Object> tokens = sessionUseCase.createSession(user.getId(), clientInfo);
        tokens.put("requiresMfa", false);
        return tokens;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmSessionReplacement(String replacementToken, boolean shouldReplace, SessionClientInfo clientInfo) {
        Map<String, Object> tokens = sessionUseCase.confirmSessionReplacement(replacementToken, shouldReplace, clientInfo);
        tokens.put("requiresMfa", false);
        return tokens;
    }

    @Override
    @Transactional
    public void logout(String sessionId) {
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setActive(false);
            sessionRepository.save(session);
            sessionCache.evictSession(sessionId);
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
        if (!StringUtils.hasText(verificationFrontendBaseUrl)) {
            return null;
        }

        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedOtp = URLEncoder.encode(otp, StandardCharsets.UTF_8);
        String baseUrl = verificationFrontendBaseUrl.endsWith("/")
                ? verificationFrontendBaseUrl.substring(0, verificationFrontendBaseUrl.length() - 1)
                : verificationFrontendBaseUrl;

        return baseUrl + "/verify-email?email=" + encodedEmail + "&otp=" + encodedOtp;
    }
}
