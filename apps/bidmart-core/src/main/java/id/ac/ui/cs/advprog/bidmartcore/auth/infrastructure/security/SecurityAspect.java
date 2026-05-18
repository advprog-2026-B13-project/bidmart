package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final HttpServletRequest request;
    private final JwtUtil jwtUtil;
    private final SessionCachePort sessionCache;
    private final SessionRepositoryPort sessionRepository;
    private final RolePermissionPort rolePermissionPort;
    private final AuthContext authContext;
    private final AuthCookieService authCookieService;

    @Around("@annotation(id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin) || " +
            "@within(id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin)")
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = resolveSession();
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        populateAuthContext(session);
        return joinPoint.proceed();
    }

    @Around("@annotation(id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequirePermission) || " +
            "@within(id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = resolveSession();
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        populateAuthContext(session);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequirePermission.class);
        }

        if (annotation != null) {
            PermissionValue[] required = annotation.value();
            boolean hasAll = Arrays.stream(required).allMatch(authContext::hasPermission);
            if (!hasAll) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Forbidden"));
            }
        }

        return joinPoint.proceed();
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
