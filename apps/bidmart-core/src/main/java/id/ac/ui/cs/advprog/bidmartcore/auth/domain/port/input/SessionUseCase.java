package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SessionUseCase {
    Map<String, Object> createSession(UUID userId);
    List<Session> listSessions(UUID userId);
    void revokeSession(UUID userId, String sessionId);
    void revokeAllOtherSessions(UUID userId, String currentSessionId);
}

