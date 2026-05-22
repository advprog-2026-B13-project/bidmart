package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock private SessionUseCase sessionUseCase;
    @Mock private AuthContext authContext;

    private SessionController controller;

    @BeforeEach
    void setUp() {
        controller = new SessionController(sessionUseCase, authContext);
    }

    private Session createSession(String id, boolean active) {
        Session s = new Session();
        s.setId(id);
        s.setActive(active);
        s.setCreatedAt(Instant.now());
        s.setLastLoginAt(Instant.now());
        s.setExpiresAt(Instant.now().plusSeconds(300));
        s.setDeviceInfo("Chrome");
        s.setIpAddress("127.0.0.1");
        s.setLocationLabel("ID");
        return s;
    }

    @Test
    void listSessions_shouldReturnOnlyActive() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        when(authContext.getSessionId()).thenReturn("current");

        Session active = createSession("active", true);
        Session inactive = createSession("inactive", false);
        when(sessionUseCase.listSessions(userId)).thenReturn(List.of(active, inactive));

        var response = controller.listSessions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("active", response.getBody().getData().get(0).getSessionId());
    }

    @Test
    void listSessions_shouldMarkCurrentSession() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        when(authContext.getSessionId()).thenReturn("s1");

        Session s1 = createSession("s1", true);
        Session s2 = createSession("s2", true);
        when(sessionUseCase.listSessions(userId)).thenReturn(List.of(s1, s2));

        var response = controller.listSessions();

        var data = response.getBody().getData();
        assertTrue(data.get(0).getIsCurrent());
        assertFalse(data.get(1).getIsCurrent());
    }

    @Test
    void revokeSession_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);

        var response = controller.revokeSession("session-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionUseCase).revokeSession(userId, "session-id");
    }

    @Test
    void revokeSession_notOwned_shouldReturn400() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalArgumentException("Not owned")).when(sessionUseCase).revokeSession(userId, "other");

        var response = controller.revokeSession("other");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void revokeAllOtherSessions_success() {
        UUID userId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(userId);
        when(authContext.getSessionId()).thenReturn("current");

        var response = controller.revokeAllOtherSessions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionUseCase).revokeAllOtherSessions(userId, "current");
    }
}
