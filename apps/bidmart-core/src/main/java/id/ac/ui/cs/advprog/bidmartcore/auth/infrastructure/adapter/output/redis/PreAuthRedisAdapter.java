package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.redis;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PreAuthSessionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PreAuthRedisAdapter implements PreAuthSessionPort {
    private static final String REDIS_PREFIX = "preauth:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String preAuthToken, UUID userId, String mfaType, long ttlSeconds) {
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("mfaType", mfaType);
        String key = REDIS_PREFIX + preAuthToken;
        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<PreAuthSessionData> get(String preAuthToken) {
        Map<Object, Object> data = redisTemplate.opsForHash().entries(REDIS_PREFIX + preAuthToken);
        if (data.isEmpty()) {
            return Optional.empty();
        }
        UUID userId = UUID.fromString((String) data.get("userId"));
        String mfaType = (String) data.get("mfaType");
        return Optional.of(new PreAuthSessionData(userId, mfaType));
    }

    @Override
    public void delete(String preAuthToken) {
        redisTemplate.delete(REDIS_PREFIX + preAuthToken);
    }
}

