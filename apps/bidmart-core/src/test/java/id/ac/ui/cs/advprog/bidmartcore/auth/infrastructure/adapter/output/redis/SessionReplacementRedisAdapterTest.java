package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.redis;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionReplacementRequestPort.SessionReplacementRequestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionReplacementRedisAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private SessionReplacementRedisAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new SessionReplacementRedisAdapter(redisTemplate);
    }

    @Test
    void unit_save_storesHashWithExpiry() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        adapter.save("repl-token", userId, "Chrome", "127.0.0.1", "Jakarta", 60L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hashOps).putAll(eq("session-replacement:repl-token"), captor.capture());
        Map<String, String> data = captor.getValue();
        assertEquals(userId.toString(), data.get("userId"));
        assertEquals("Chrome", data.get("deviceInfo"));
        assertEquals("127.0.0.1", data.get("ipAddress"));
        assertEquals("Jakarta", data.get("locationLabel"));
        verify(redisTemplate).expire("session-replacement:repl-token", 60L, TimeUnit.SECONDS);
    }

    @Test
    void unit_get_returnsDataWhenPresent() {
        UUID userId = UUID.randomUUID();
        Map<Object, Object> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("deviceInfo", "Chrome");
        data.put("ipAddress", "10.0.0.1");
        data.put("locationLabel", "Bandung");

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("session-replacement:repl-token")).thenReturn(data);

        Optional<SessionReplacementRequestData> result = adapter.get("repl-token");

        assertTrue(result.isPresent());
        SessionReplacementRequestData req = result.get();
        assertEquals(userId, req.userId());
        assertEquals("Chrome", req.deviceInfo());
        assertEquals("10.0.0.1", req.ipAddress());
        assertEquals("Bandung", req.locationLabel());
    }

    @Test
    void unit_get_returnsEmptyWhenNotPresent() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("session-replacement:repl-token")).thenReturn(Map.of());

        Optional<SessionReplacementRequestData> result = adapter.get("repl-token");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_delete_removesKey() {
        adapter.delete("repl-token");

        verify(redisTemplate).delete("session-replacement:repl-token");
    }
}
