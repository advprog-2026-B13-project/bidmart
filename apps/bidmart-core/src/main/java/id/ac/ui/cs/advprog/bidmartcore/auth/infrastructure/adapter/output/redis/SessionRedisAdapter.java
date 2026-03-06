package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.redis;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SessionRedisAdapter implements SessionCachePort {
    private static final String REDIS_PREFIX = "session:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void cacheSession(String sessionId, Session session, long ttlMillis) {
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + sessionId,
                session,
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public Optional<Session> getCachedSession(String sessionId) {
        Object cached = redisTemplate.opsForValue().get(REDIS_PREFIX + sessionId);
        if (cached instanceof Session session) {
            return Optional.of(session);
        }
        return Optional.empty();
    }

    @Override
    public void evictSession(String sessionId) {
        redisTemplate.delete(REDIS_PREFIX + sessionId);
    }
}

