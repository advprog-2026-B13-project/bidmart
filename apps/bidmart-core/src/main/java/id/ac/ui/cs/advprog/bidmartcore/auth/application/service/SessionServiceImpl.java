package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionUseCase {

    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;
    private final UserRepositoryPort userRepository;
    private final JwtUtil jwtUtil;

    @Value("${auth.session.max-active:5}")
    private int maxActiveSessions;

    @Override
    @Transactional
    public Map<String, Object> createSession(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Enforce session limit: deactivate oldest if limit reached
        List<Session> existing = sessionRepository.findAllByUserId(userId);
        long activeCount = existing.stream().filter(Session::isActive).count();
        if (activeCount >= maxActiveSessions) {
            existing.stream()
                    .filter(Session::isActive)
                    .min(Comparator.comparing(Session::getExpiresAt))
                    .ifPresent(oldest -> {
                        oldest.setActive(false);
                        sessionRepository.save(oldest);
                        sessionCache.evictSession(oldest.getId());
                    });
        }

        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setActive(true);

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
        return result;
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
}

