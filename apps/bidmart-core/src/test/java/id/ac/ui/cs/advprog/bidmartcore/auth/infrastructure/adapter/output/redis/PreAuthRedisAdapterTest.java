package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.redis;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PreAuthSessionPort.PreAuthSessionData;
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
class PreAuthRedisAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private PreAuthRedisAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new PreAuthRedisAdapter(redisTemplate);
    }

    @Test
    void unit_save_storesHashWithExpiry() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        adapter.save("token-abc", userId, "TOTP", 300L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hashOps).putAll(eq("preauth:token-abc"), captor.capture());
        Map<String, String> data = captor.getValue();
        assertEquals(userId.toString(), data.get("userId"));
        assertEquals("TOTP", data.get("mfaType"));
        verify(redisTemplate).expire("preauth:token-abc", 300L, TimeUnit.SECONDS);
    }

    @Test
    void unit_get_returnsDataWhenPresent() {
        UUID userId = UUID.randomUUID();
        Map<Object, Object> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("mfaType", "EMAIL");

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("preauth:token-abc")).thenReturn(data);

        Optional<PreAuthSessionData> result = adapter.get("token-abc");

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().userId());
        assertEquals("EMAIL", result.get().mfaType());
    }

    @Test
    void unit_get_returnsEmptyWhenNotPresent() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("preauth:token-abc")).thenReturn(Map.of());

        Optional<PreAuthSessionData> result = adapter.get("token-abc");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_delete_removesKey() {
        adapter.delete("token-abc");

        verify(redisTemplate).delete("preauth:token-abc");
    }
}
