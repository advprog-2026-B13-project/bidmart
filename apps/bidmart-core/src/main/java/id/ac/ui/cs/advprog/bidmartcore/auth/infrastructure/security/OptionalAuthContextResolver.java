package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionalAuthContextResolver {

    private final HttpServletRequest request;
    private final JwtUtil jwtUtil;
    private final SessionCachePort sessionCache;
    private final SessionRepositoryPort sessionRepository;
    private final RolePermissionPort rolePermissionPort;
    private final AuthContext authContext;
    private final AuthCookieService authCookieService;

    public Optional<UUID> populateIfPresent() {
        Session session = resolveSession();
        if (session == null) {
            return Optional.empty();
        }

        populateAuthContext(session);
        return Optional.ofNullable(authContext.getUserId());
    }

    private Session resolveSession() {
        String token = authCookieService.resolveAccessToken(request)
                .orElseGet(this::extractBearerToken);

        if (token == null) {
            return null;
        }

        if (!jwtUtil.isTokenValid(token) || jwtUtil.isRefreshToken(token)) {
            return null;
        }

        String sessionId;
        try {
            sessionId = jwtUtil.extractSessionId(token);
        } catch (Exception e) {
            return null;
        }

        Session session = sessionCache.getCachedSession(sessionId)
                .orElseGet(() -> {
                    Session fromDb = sessionRepository.findById(sessionId).orElse(null);
                    if (fromDb != null) {
                        long ttl = fromDb.getExpiresAt().toEpochMilli() - System.currentTimeMillis();
                        if (ttl > 0) {
                            sessionCache.cacheSession(sessionId, fromDb, ttl);
                        }
                    }
                    return fromDb;
                });

        if (session == null || !session.isActive() || session.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }

        User user = session.getUser();
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }

        return session;
    }

    private String extractBearerToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private void populateAuthContext(Session session) {
        User user = session.getUser();
        authContext.setUserId(user.getId());
        authContext.setSessionId(session.getId());
        authContext.setUser(user);
        authContext.setAuthenticated(true);

        if (user.getRole() != null) {
            Set<PermissionValue> permissions = rolePermissionPort.findPermissionsByRoleId(user.getRole().getId());
            authContext.setPermissions(permissions);
        } else {
            authContext.setPermissions(Set.of());
        }
    }
}
