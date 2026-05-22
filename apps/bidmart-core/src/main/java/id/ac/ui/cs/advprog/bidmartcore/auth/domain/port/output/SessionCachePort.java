package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;

import java.util.Optional;

public interface SessionCachePort {
    void cacheSession(String sessionId, Session session, long ttlMillis);
    Optional<Session> getCachedSession(String sessionId);
    void evictSession(String sessionId);
}

