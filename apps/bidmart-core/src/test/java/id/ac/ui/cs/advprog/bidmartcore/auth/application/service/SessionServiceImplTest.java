package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.SessionClientInfo;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionReplacementRequestPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionReplacementRequestPort.SessionReplacementRequestData;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepositoryPort sessionRepository;

    @Mock
    private SessionCachePort sessionCache;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private SessionReplacementRequestPort sessionReplacementRequestPort;

    @Mock
    private JwtUtil jwtUtil;

    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl(
                sessionRepository,
                sessionCache,
                userRepository,
                sessionReplacementRequestPort,
                jwtUtil
        );
        ReflectionTestUtils.setField(service, "maxActiveSessions", 2);
        ReflectionTestUtils.setField(service, "sessionLimitStrategy", "AUTO_REVOKE");
        ReflectionTestUtils.setField(service, "replacementTokenTtlSeconds", 300L);
    }

    @Test
    void createSession_whenUserMissing_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createSession(userId, SessionClientInfo.unknown()));
    }

    @Test
    void createSession_whenUnderLimit_shouldCreateAndCache() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of());

        JwtToken access = new JwtToken();
        access.setToken("access");
        access.setExpirationTime(Instant.now().plusSeconds(60));
        JwtToken refresh = new JwtToken();
        refresh.setToken("refresh");
        refresh.setExpirationTime(Instant.now().plusSeconds(300));
        when(jwtUtil.generateAccessToken(anyString())).thenReturn(access);
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn(refresh);

        Map<String, Object> result = service.createSession(userId, SessionClientInfo.unknown());

        assertEquals("access", result.get("accessToken"));
        assertEquals(false, result.get("requiresSessionReplacement"));
        verify(sessionRepository).save(any(Session.class));
        verify(sessionCache).cacheSession(anyString(), any(Session.class), anyLong());
    }

    @Test
    void createSession_whenLimitReachedWithConfirmation_shouldReturnPending() {
        ReflectionTestUtils.setField(service, "sessionLimitStrategy", "CONFIRMATION_TOKEN");
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Session active1 = new Session();
        active1.setId("active-1");
        active1.setActive(true);
        active1.setCreatedAt(Instant.now().minusSeconds(120));

        Session active2 = new Session();
        active2.setId("active-2");
        active2.setActive(true);
        active2.setCreatedAt(Instant.now().minusSeconds(60));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of(active1, active2));

        Map<String, Object> result = service.createSession(userId, SessionClientInfo.unknown());

        assertEquals(true, result.get("requiresSessionReplacement"));
        assertNotNull(result.get("sessionReplacementToken"));
        verify(sessionReplacementRequestPort).save(anyString(), eq(userId), anyString(), anyString(), anyString(), eq(300L));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void confirmSessionReplacement_whenCanceled_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(sessionReplacementRequestPort.get("token"))
                .thenReturn(Optional.of(new SessionReplacementRequestData(userId, null, null, null)));

        assertThrows(IllegalArgumentException.class,
                () -> service.confirmSessionReplacement("token", false, SessionClientInfo.unknown()));
    }

    @Test
    void confirmSessionReplacement_whenValid_shouldCreateSession() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(sessionReplacementRequestPort.get("token"))
                .thenReturn(Optional.of(new SessionReplacementRequestData(userId, "device", "ip", "loc")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of());

        JwtToken access = new JwtToken();
        access.setToken("access");
        access.setExpirationTime(Instant.now().plusSeconds(60));
        JwtToken refresh = new JwtToken();
        refresh.setToken("refresh");
        refresh.setExpirationTime(Instant.now().plusSeconds(300));
        when(jwtUtil.generateAccessToken(anyString())).thenReturn(access);
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn(refresh);

        Map<String, Object> result = service.confirmSessionReplacement("token", true, SessionClientInfo.unknown());

        assertEquals("access", result.get("accessToken"));
        verify(sessionReplacementRequestPort).delete("token");
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void revokeSession_whenNotOwned_shouldThrow() {
        UUID userId = UUID.randomUUID();
        User other = new User();
        other.setId(UUID.randomUUID());
        Session session = new Session();
        session.setId("session-id");
        session.setUser(other);
        session.setActive(true);

        when(sessionRepository.findById("session-id")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> service.revokeSession(userId, "session-id"));
    }

    @Test
    void revokeAllOtherSessions_shouldDeactivateOthers() {
        UUID userId = UUID.randomUUID();
        Session current = new Session();
        current.setId("current");
        current.setActive(true);
        Session other = new Session();
        other.setId("other");
        other.setActive(true);

        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of(current, other));

        service.revokeAllOtherSessions(userId, "current");

        assertFalse(other.isActive());
        verify(sessionRepository).save(other);
        verify(sessionCache).evictSession("other");
    }

    @Test
    void createSession_whenAutoRevoke_shouldDeactivateOldest() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Session oldest = new Session();
        oldest.setId("oldest");
        oldest.setActive(true);
        oldest.setCreatedAt(Instant.now().minusSeconds(300));
        Session newest = new Session();
        newest.setId("newest");
        newest.setActive(true);
        newest.setCreatedAt(Instant.now().minusSeconds(100));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of(oldest, newest));

        JwtToken access = new JwtToken();
        access.setToken("access");
        access.setExpirationTime(Instant.now().plusSeconds(60));
        JwtToken refresh = new JwtToken();
        refresh.setToken("refresh");
        refresh.setExpirationTime(Instant.now().plusSeconds(300));
        when(jwtUtil.generateAccessToken(anyString())).thenReturn(access);
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn(refresh);

        service.createSession(userId, SessionClientInfo.unknown());

        assertFalse(oldest.isActive());
        verify(sessionRepository).save(oldest);
        verify(sessionCache).evictSession("oldest");
    }
}

