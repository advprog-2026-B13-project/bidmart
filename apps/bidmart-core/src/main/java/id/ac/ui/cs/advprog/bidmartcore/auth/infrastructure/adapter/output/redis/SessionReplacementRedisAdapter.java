package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.redis;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionReplacementRequestPort;
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
public class SessionReplacementRedisAdapter implements SessionReplacementRequestPort {
    private static final String REDIS_PREFIX = "session-replacement:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String replacementToken, UUID userId, String deviceInfo, String ipAddress, String locationLabel, long ttlSeconds) {
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("deviceInfo", deviceInfo);
        data.put("ipAddress", ipAddress);
        data.put("locationLabel", locationLabel);

        String key = REDIS_PREFIX + replacementToken;
        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<SessionReplacementRequestData> get(String replacementToken) {
        Map<Object, Object> data = redisTemplate.opsForHash().entries(REDIS_PREFIX + replacementToken);
        if (data.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SessionReplacementRequestData(
                UUID.fromString((String) data.get("userId")),
                (String) data.get("deviceInfo"),
                (String) data.get("ipAddress"),
                (String) data.get("locationLabel")
        ));
    }

    @Override
    public void delete(String replacementToken) {
        redisTemplate.delete(REDIS_PREFIX + replacementToken);
    }
}

