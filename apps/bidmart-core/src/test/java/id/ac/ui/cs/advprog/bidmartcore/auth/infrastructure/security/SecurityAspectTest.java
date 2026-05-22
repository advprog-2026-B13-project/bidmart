package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAspectTest {

    @Mock private HttpServletRequest request;
    @Mock private JwtUtil jwtUtil;
    @Mock private SessionCachePort sessionCache;
    @Mock private SessionRepositoryPort sessionRepository;
    @Mock private RolePermissionPort rolePermissionPort;
    @Mock private AuthContext authContext;
    @Mock private AuthCookieService authCookieService;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature methodSignature;

    private SecurityAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new SecurityAspect(request, jwtUtil, sessionCache, sessionRepository, rolePermissionPort, authContext, authCookieService);
    }

    private Session createValidSession() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setId(1);
        role.setName("USER");
        user.setRole(role);

        Session session = new Session();
        session.setId("session-id");
        session.setUser(user);
        session.setActive(true);
        session.setExpiresAt(Instant.now().plusSeconds(300));
        return session;
    }

    @Test
    void checkLogin_noToken_shouldReturnUnauthorized() throws Throwable {
        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.empty());
        when(request.getHeader("Authorization")).thenReturn(null);

        Object result = aspect.checkLogin(joinPoint);

        assertInstanceOf(ResponseEntity.class, result);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void checkLogin_invalidToken_shouldReturnUnauthorized() throws Throwable {
        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("bad-token"));
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

        Object result = aspect.checkLogin(joinPoint);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @Test
    void checkLogin_refreshTokenUsed_shouldReturnUnauthorized() throws Throwable {
        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("refresh-token"));
        when(jwtUtil.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);

        Object result = aspect.checkLogin(joinPoint);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @Test
    void checkLogin_sessionNotInCache_foundInDb_shouldWork() throws Throwable {
        Session session = createValidSession();

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.empty());
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkLogin(joinPoint);

        assertEquals("ok", result);
        verify(sessionCache).cacheSession(eq("session-id"), eq(session), anyLong());
    }

    @Test
    void checkLogin_validToken_shouldProceed() throws Throwable {
        Session session = createValidSession();

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkLogin(joinPoint);

        assertEquals("ok", result);
        verify(authContext).setUserId(session.getUser().getId());
        verify(authContext).setSessionId("session-id");
        verify(authContext).setAuthenticated(true);
    }

    @Test
    void checkLogin_inactiveSession_shouldReturnUnauthorized() throws Throwable {
        Session session = createValidSession();
        session.setActive(false);

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));

        Object result = aspect.checkLogin(joinPoint);

        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @Test
    void checkLogin_expiredSession_shouldReturnUnauthorized() throws Throwable {
        Session session = createValidSession();
        session.setExpiresAt(Instant.now().minusSeconds(10));

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));

        Object result = aspect.checkLogin(joinPoint);

        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @Test
    void checkLogin_suspendedUser_shouldReturnUnauthorized() throws Throwable {
        Session session = createValidSession();
        session.getUser().setStatus(UserStatus.SUSPENDED);

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));

        Object result = aspect.checkLogin(joinPoint);

        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @Test
    void checkLogin_bearerToken_shouldWork() throws Throwable {
        Session session = createValidSession();

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.empty());
        when(request.getHeader("Authorization")).thenReturn("Bearer bearer-token");
        when(jwtUtil.isTokenValid("bearer-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("bearer-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("bearer-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkLogin(joinPoint);

        assertEquals("ok", result);
    }

    @Test
    void checkPermission_noSession_shouldReturnUnauthorized() throws Throwable {
        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.empty());
        when(request.getHeader("Authorization")).thenReturn(null);

        Object result = aspect.checkPermission(joinPoint);

        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkPermission_missingPermission_shouldReturnForbidden() throws Throwable {
        Session session = createValidSession();

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));
        when(rolePermissionPort.findPermissionsByRoleId(1)).thenReturn(Set.of());

        when(authContext.hasPermission(any(PermissionValue.class))).thenReturn(false);

        Method method = DummyController.class.getMethod("securedMethod");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);

        Object result = aspect.checkPermission(joinPoint);

        assertEquals(HttpStatus.FORBIDDEN, ((ResponseEntity<?>) result).getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkPermission_hasPermission_shouldProceed() throws Throwable {
        Session session = createValidSession();

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));
        when(rolePermissionPort.findPermissionsByRoleId(1)).thenReturn(Set.of(PermissionValue.ADMIN));

        when(authContext.hasPermission(any(PermissionValue.class))).thenReturn(true);

        Method method = DummyController.class.getMethod("securedMethod");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkPermission(joinPoint);

        assertEquals("ok", result);
    }

    @Test
    void populateOptionalLogin_noToken_shouldProceed() throws Throwable {
        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.empty());
        when(request.getHeader("Authorization")).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.populateOptionalLogin(joinPoint);

        assertEquals("ok", result);
        verify(authContext, never()).setAuthenticated(true);
    }

    @Test
    void populateOptionalLogin_validToken_shouldPopulateContext() throws Throwable {
        Session session = createValidSession();

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.populateOptionalLogin(joinPoint);

        assertEquals("ok", result);
        verify(authContext).setAuthenticated(true);
    }

    @Test
    void checkLogin_userWithNoRole_shouldSetEmptyPermissions() throws Throwable {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(null);

        Session session = new Session();
        session.setId("session-id");
        session.setUser(user);
        session.setActive(true);
        session.setExpiresAt(Instant.now().plusSeconds(300));

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.of(session));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkLogin(joinPoint);

        assertEquals("ok", result);
        verify(authContext).setPermissions(Set.of());
    }

    @Test
    void checkLogin_extractSessionIdThrows_shouldReturnUnauthorized() throws Throwable {
        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenThrow(new RuntimeException("parse error"));

        Object result = aspect.checkLogin(joinPoint);

        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
    }

    @Test
    void checkLogin_sessionFromDbExpiredTtl_shouldNotCache() throws Throwable {
        Session session = createValidSession();
        session.setExpiresAt(Instant.now().minusSeconds(10));

        when(authCookieService.resolveAccessToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid-token")).thenReturn(false);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("session-id");
        when(sessionCache.getCachedSession("session-id")).thenReturn(Optional.empty());
        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        Object result = aspect.checkLogin(joinPoint);

        assertEquals(HttpStatus.UNAUTHORIZED, ((ResponseEntity<?>) result).getStatusCode());
        verify(sessionCache, never()).cacheSession(anyString(), any(), anyLong());
    }

    public static class DummyController {
        @RequirePermission(PermissionValue.ADMIN)
        public String securedMethod() { return "secured"; }
    }
}
