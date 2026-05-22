package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.redis;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRedisAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private SessionRedisAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new SessionRedisAdapter(redisTemplate);
    }

    @Test
    void unit_cacheSession_storesWithPrefix() {
        Session session = new Session();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        adapter.cacheSession("sess-123", session, 60000L);

        verify(valueOps).set(eq("session:sess-123"), eq(session), eq(60000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void unit_getCachedSession_returnsSessionWhenCached() {
        Session session = new Session();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("session:sess-123")).thenReturn(session);

        Optional<Session> result = adapter.getCachedSession("sess-123");

        assertTrue(result.isPresent());
        assertSame(session, result.get());
    }

    @Test
    void unit_getCachedSession_returnsEmptyWhenNotCached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("session:sess-123")).thenReturn(null);

        Optional<Session> result = adapter.getCachedSession("sess-123");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_getCachedSession_returnsEmptyWhenNotSessionType() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("session:sess-123")).thenReturn("not-a-session");

        Optional<Session> result = adapter.getCachedSession("sess-123");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_evictSession_deletesKey() {
        adapter.evictSession("sess-123");

        verify(redisTemplate).delete("session:sess-123");
    }
}
