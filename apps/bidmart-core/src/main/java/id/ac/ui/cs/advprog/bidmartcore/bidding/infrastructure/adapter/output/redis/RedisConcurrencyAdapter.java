package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisConcurrencyAdapter implements ConcurrencyPort {

    private final StringRedisTemplate redis;
    private final RedisScript<List> placeBidScript;
    private final RedisScript<Long> rollbackScript;

    private static final String KEY_PREFIX = "auction:";

    private String key(UUID listingId) {
        return KEY_PREFIX + listingId + ":state";
    }

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Jakarta");

    private long toEpochMillis(java.time.LocalDateTime ldt) {
        return ldt.toInstant(BUSINESS_ZONE.getRules().getOffset(ldt)).toEpochMilli();
    }

    @Override
    public ConcurrencyResult placeBid(UUID listingId, long bidAmount, long minIncrement,
                                     UUID bidderId, long currentTimeMillis,
                                     long antiSnipeThresholdMillis, long antiSnipeExtensionMillis) {
        List<Object> result = redis.execute(placeBidScript,
                Collections.singletonList(key(listingId)),
                String.valueOf(bidAmount),
                String.valueOf(minIncrement),
                bidderId.toString(),
                String.valueOf(currentTimeMillis),
                String.valueOf(antiSnipeThresholdMillis),
                String.valueOf(antiSnipeExtensionMillis));

        long status = ((Number) result.get(0)).longValue();
        long oldPrice = ((Number) result.get(1)).longValue();
        String oldWinner = result.get(2) != null ? String.valueOf(result.get(2)) : "";
        long oldEndTime = ((Number) result.get(3)).longValue();

        return switch ((int) status) {
            case 1  -> ConcurrencyResult.accepted(oldPrice, oldWinner, oldEndTime);
            case 0  -> ConcurrencyResult.rejected(oldPrice, oldWinner, oldEndTime);
            case -1 -> ConcurrencyResult.cacheMiss();
            case -2 -> ConcurrencyResult.notActive();
            case -3 -> ConcurrencyResult.ended();
            default -> ConcurrencyResult.cacheMiss();
        };
    }

    @Override
    public void rollback(UUID listingId, long priceToRestore, String winnerToRestore, long endTimeToRestore) {
        redis.execute(rollbackScript,
                Collections.singletonList(key(listingId)),
                String.valueOf(priceToRestore),
                winnerToRestore == null ? "" : winnerToRestore,
                String.valueOf(endTimeToRestore));
    }

    @Override
    public void cacheAuction(UUID listingId, ListingInfo info) {
        String redisKey = key(listingId);

        Boolean inserted = redis.opsForHash().putIfAbsent(redisKey, "status", info.status().name());
        if (Boolean.FALSE.equals(inserted)) {
            return; // already cached, skip
        }

        long price = info.currentPrice() != null
                ? info.currentPrice().longValue()
                : info.startingPrice().longValue();
        long endTime = toEpochMillis(info.endTime());

        redis.opsForHash().putAll(redisKey, Map.of(
                "price", String.valueOf(price),
                "endTime", String.valueOf(endTime),
                "status", info.status().name(),
                "winner", ""
        ));
    }

    @Override
    public void refreshAuction(UUID listingId, ListingInfo info) {
        long endTime = toEpochMillis(info.endTime());
        long price = info.currentPrice() != null
                ? info.currentPrice().longValue()
                : info.startingPrice().longValue();
        redis.opsForHash().putAll(key(listingId), Map.of(
                "price", String.valueOf(price),
                "endTime", String.valueOf(endTime),
                "status", info.status().name(),
                "winner", info.winnerId() != null ? info.winnerId().toString() : ""
        ));
    }

    @Override
    public void removeAuction(UUID listingId) {
        redis.delete(key(listingId));
    }

    @Override
    public long getAuctionEndTime(UUID listingId) {
        Object endTime = redis.opsForHash().get(key(listingId), "endTime");
        return endTime != null ? Long.parseLong(endTime.toString()) : 0L;
    }
}
