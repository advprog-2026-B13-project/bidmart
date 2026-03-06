package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${auth.mfa.pre-auth-ttl-seconds:300}")
    private long preAuthTtlSeconds;

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
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", saved.getId());
        result.put("email", saved.getEmail());
        result.put("displayName", saved.getDisplayName());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Account is suspended");
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
        Map<String, Object> tokens = sessionUseCase.createSession(user.getId());
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
        sessionRepository.save(session);

        long redisTtl = newRefreshToken.getExpirationTime().toEpochMilli() - System.currentTimeMillis();
        sessionCache.cacheSession(sessionId, session, redisTtl);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken.getToken());
        result.put("refreshToken", newRefreshToken.getToken());
        return result;
    }
}

