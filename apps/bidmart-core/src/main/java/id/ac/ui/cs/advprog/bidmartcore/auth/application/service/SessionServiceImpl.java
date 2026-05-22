package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionReplacementRequestPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionReplacementRequestPort.SessionReplacementRequestData;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionUseCase {

    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;
    private final UserRepositoryPort userRepository;
    private final SessionReplacementRequestPort sessionReplacementRequestPort;
    private final JwtUtil jwtUtil;

    private enum SessionLimitStrategy {
        AUTO_REVOKE,
        CONFIRMATION_TOKEN
    }

    @Value("${auth.session.max-active:3}")
    private int maxActiveSessions;

    @Value("${auth.session.limit-strategy:AUTO_REVOKE}")
    private String sessionLimitStrategy;

    @Value("${auth.session.replacement-token-ttl-seconds:300}")
    private long replacementTokenTtlSeconds;

    @Override
    @Transactional
    public Map<String, Object> createSession(UUID userId, SessionClientInfo clientInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return createSessionInternal(user, normalizeClientInfo(clientInfo), false);
    }

    @Override
    @Transactional
    public Map<String, Object> confirmSessionReplacement(String replacementToken, boolean shouldReplace, SessionClientInfo fallbackClientInfo) {
        SessionReplacementRequestData replacementRequest = sessionReplacementRequestPort.get(replacementToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired session replacement token"));

        sessionReplacementRequestPort.delete(replacementToken);

        if (!shouldReplace) {
            throw new IllegalArgumentException("Login canceled because active session limit was reached");
        }

        User user = userRepository.findById(replacementRequest.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SessionClientInfo safeFallback = normalizeClientInfo(fallbackClientInfo);
        SessionClientInfo clientInfo = SessionClientInfo.of(
                replacementRequest.deviceInfo() != null ? replacementRequest.deviceInfo() : safeFallback.deviceInfo(),
                replacementRequest.ipAddress() != null ? replacementRequest.ipAddress() : safeFallback.ipAddress(),
                replacementRequest.locationLabel() != null ? replacementRequest.locationLabel() : safeFallback.locationLabel()
        );

        return createSessionInternal(user, clientInfo, true);
    }

    @Override
    public List<Session> listSessions(UUID userId) {
        return sessionRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to user");
        }

        session.setActive(false);
        sessionRepository.save(session);
        sessionCache.evictSession(sessionId);
    }

    @Override
    @Transactional
    public void revokeAllOtherSessions(UUID userId, String currentSessionId) {
        List<Session> sessions = sessionRepository.findAllByUserId(userId);
        for (Session s : sessions) {
            if (!s.getId().equals(currentSessionId) && s.isActive()) {
                s.setActive(false);
                sessionRepository.save(s);
                sessionCache.evictSession(s.getId());
            }
        }
    }

    private Map<String, Object> createSessionInternal(User user, SessionClientInfo clientInfo, boolean forceReplaceOldest) {
        List<Session> existing = sessionRepository.findAllByUserId(user.getId());
        List<Session> activeSessions = existing.stream().filter(Session::isActive).toList();

        if (activeSessions.size() >= maxActiveSessions) {
            SessionLimitStrategy strategy = resolveLimitStrategy();

            if (strategy == SessionLimitStrategy.CONFIRMATION_TOKEN && !forceReplaceOldest) {
                String replacementToken = UUID.randomUUID().toString();
                sessionReplacementRequestPort.save(
                        replacementToken,
                        user.getId(),
                        clientInfo.deviceInfo(),
                        clientInfo.ipAddress(),
                        clientInfo.locationLabel(),
                        replacementTokenTtlSeconds
                );

                Map<String, Object> pending = new HashMap<>();
                pending.put("requiresSessionReplacement", true);
                pending.put("sessionReplacementToken", replacementToken);
                pending.put("activeSessions", buildActiveSessionPayload(activeSessions));
                pending.put("requiresMfa", false);
                return pending;
            }

            deactivateOldestActiveSession(activeSessions);
        }

        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setActive(true);

        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setLastLoginAt(now);
        session.setDeviceInfo(clientInfo.deviceInfo());
        session.setIpAddress(clientInfo.ipAddress());
        session.setLocationLabel(clientInfo.locationLabel());

        JwtToken accessToken = jwtUtil.generateAccessToken(session.getId());
        JwtToken refreshToken = jwtUtil.generateRefreshToken(session.getId());

        session.setExpiresAt(refreshToken.getExpirationTime());
        session.setRefreshToken(refreshToken.getToken());

        sessionRepository.save(session);

        long redisTtl = refreshToken.getExpirationTime().toEpochMilli() - System.currentTimeMillis();
        sessionCache.cacheSession(session.getId(), session, redisTtl);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken.getToken());
        result.put("refreshToken", refreshToken.getToken());
        result.put("requiresSessionReplacement", false);
        return result;
    }

    private void deactivateOldestActiveSession(List<Session> activeSessions) {
        activeSessions.stream()
                .min(Comparator.comparing(this::sortTimestamp))
                .ifPresent(oldest -> {
                    oldest.setActive(false);
                    sessionRepository.save(oldest);
                    sessionCache.evictSession(oldest.getId());
                });
    }

    private Instant sortTimestamp(Session session) {
        if (session.getLastLoginAt() != null) {
            return session.getLastLoginAt();
        }
        if (session.getCreatedAt() != null) {
            return session.getCreatedAt();
        }
        return session.getExpiresAt();
    }

    private SessionLimitStrategy resolveLimitStrategy() {
        try {
            return SessionLimitStrategy.valueOf(sessionLimitStrategy.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return SessionLimitStrategy.AUTO_REVOKE;
        }
    }

    private SessionClientInfo normalizeClientInfo(SessionClientInfo clientInfo) {
        return clientInfo == null ? SessionClientInfo.unknown() : clientInfo;
    }

    private List<Map<String, Object>> buildActiveSessionPayload(List<Session> activeSessions) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Session session : activeSessions) {
            Map<String, Object> item = new HashMap<>();
            item.put("sessionId", session.getId());
            item.put("lastLoginAt", session.getLastLoginAt() != null ? session.getLastLoginAt().toString() : null);
            item.put("expiresAt", session.getExpiresAt() != null ? session.getExpiresAt().toString() : null);
            item.put("deviceInfo", session.getDeviceInfo());
            item.put("ipAddress", session.getIpAddress());
            item.put("locationLabel", session.getLocationLabel());
            payload.add(item);
        }
        return payload;
    }
}
