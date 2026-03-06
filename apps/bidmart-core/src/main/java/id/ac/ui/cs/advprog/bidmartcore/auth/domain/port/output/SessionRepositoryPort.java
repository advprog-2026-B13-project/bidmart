package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepositoryPort {
    Session save(Session session);
    Optional<Session> findById(String sessionId);
    List<Session> findAllByUserId(UUID userId);
    void deleteById(String sessionId);
    void deleteAllByUserIdExcept(UUID userId, String currentSessionId);
    int countActiveByUserId(UUID userId);
}

