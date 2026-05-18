package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidType;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisConcurrencyAdapter implements ConcurrencyPort {

    private final StringRedisTemplate redis;
    private final BidRepositoryPort bidRepository;
    private final RedisScript<List<Object>> placeBidScript;
    private final RedisScript<Long> rollbackScript;

    private static final String KEY_PREFIX = "auction:";
    private static final String EXPIRY_SET_KEY = "auction:expiry";

    private String key(UUID listingId) {
        return KEY_PREFIX + listingId + ":state";
    }

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Jakarta");

    private long toEpochMillis(java.time.LocalDateTime ldt) {
        return ldt.toInstant(BUSINESS_ZONE.getRules().getOffset(ldt)).toEpochMilli();
    }

    @Override
    public ConcurrencyResult placeBid(UUID listingId, long bidAmount, long minIncrement,
            UUID bidderId, BidType bidType, long currentTimeMillis,
                                     long antiSnipeThresholdMillis, long antiSnipeExtensionMillis) {
        List<Object> result = redis.execute(placeBidScript,
                Collections.singletonList(key(listingId)),
                String.valueOf(bidAmount),
                String.valueOf(minIncrement),
                bidderId.toString(),
                bidType.name(),
                String.valueOf(currentTimeMillis),
                String.valueOf(antiSnipeThresholdMillis),
                String.valueOf(antiSnipeExtensionMillis));

        if (result == null || result.isEmpty()) {
            return ConcurrencyResult.cacheMiss();
        }

        long status = ((Number) result.get(0)).longValue();
        long visiblePrice = parseLong(result, 1);
        String winnerId = parseString(result, 2);
        long endTime = parseLong(result, 3);
        long bidderCommittedMax = parseLong(result, 4);
        long proxyVisiblePrice = parseLong(result, 5);
        String proxyWinnerId = parseString(result, 6);

        return switch ((int) status) {
            case 1 -> ConcurrencyResult.leading(
                visiblePrice,
                winnerId,
                endTime,
                bidderCommittedMax);
            case -4 -> ConcurrencyResult.outbid(
                visiblePrice,
                winnerId,
                endTime,
                bidderCommittedMax,
                proxyVisiblePrice,
                proxyWinnerId);
            case 0  -> ConcurrencyResult.rejected();
            case -1 -> ConcurrencyResult.cacheMiss();
            case -2 -> ConcurrencyResult.notActive();
            case -3 -> ConcurrencyResult.ended();
            default -> ConcurrencyResult.cacheMiss();
        };
    }

    private long parseLong(List<Object> result, int index) {
        if (result.size() <= index || result.get(index) == null) {
            return 0L;
        }
        Object value = result.get(index);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String parseString(List<Object> result, int index) {
        if (result.size() <= index || result.get(index) == null) {
            return "";
        }
        return String.valueOf(result.get(index));
    }

    @Override
    public void rollback(UUID listingId,
                         long priceToRestore,
                         String winnerToRestore,
                         long endTimeToRestore,
                         long expectedCurrentPrice,
                         String expectedCurrentWinner) {
        redis.execute(rollbackScript,
                Collections.singletonList(key(listingId)),
                String.valueOf(priceToRestore),
                winnerToRestore == null ? "" : winnerToRestore,
                String.valueOf(endTimeToRestore),
                String.valueOf(expectedCurrentPrice),
                expectedCurrentWinner == null ? "" : expectedCurrentWinner);
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
        String winner = info.winnerId() != null ? info.winnerId().toString() : "";
        long maxAmount = winner.isBlank() ? price : resolveHighestMaxAmount(listingId, price);

        redis.opsForHash().putAll(redisKey, Map.of(
                "price", String.valueOf(price),
                "endTime", String.valueOf(endTime),
                "status", info.status().name(),
                "winner", winner,
                "maxAmount", String.valueOf(maxAmount)
        ));

        // Add to expiry sorted set
        addToExpirySet(listingId, endTime);
    }

    @Override
    public void refreshAuction(UUID listingId, ListingInfo info) {
        long endTime = toEpochMillis(info.endTime());
        long price = info.currentPrice() != null
                ? info.currentPrice().longValue()
                : info.startingPrice().longValue();
        String winner = info.winnerId() != null ? info.winnerId().toString() : "";
        long maxAmount = winner.isBlank() ? price : resolveHighestMaxAmount(listingId, price);
        redis.opsForHash().putAll(key(listingId), Map.of(
                "price", String.valueOf(price),
                "endTime", String.valueOf(endTime),
                "status", info.status().name(),
                "winner", winner,
                "maxAmount", String.valueOf(maxAmount)
        ));

        // Update expiry sorted set (anti-snipe extends end time)
        addToExpirySet(listingId, endTime);
    }

    private long resolveHighestMaxAmount(UUID listingId, long fallbackPrice) {
        return bidRepository.findTopBid(listingId)
                .map(bid -> bid.getMaxAmount() != null ? bid.getMaxAmount().longValue() : bid.getAmount().longValue())
                .orElse(fallbackPrice);
    }

    @Override
    public void removeAuction(UUID listingId) {
        redis.delete(key(listingId));
        removeFromExpirySet(listingId);
    }

    @Override
    public long getAuctionEndTime(UUID listingId) {
        Object endTime = redis.opsForHash().get(key(listingId), "endTime");
        return endTime != null ? Long.parseLong(endTime.toString()) : 0L;
    }

    @Override
    public void addToExpirySet(UUID listingId, long endTimeEpochMillis) {
        redis.opsForZSet().add(EXPIRY_SET_KEY, listingId.toString(), endTimeEpochMillis);
    }

    @Override
    public void removeFromExpirySet(UUID listingId) {
        redis.opsForZSet().remove(EXPIRY_SET_KEY, listingId.toString());
    }

    @Override
    public java.util.Set<String> getExpiredFromExpirySet(long upToEpochMillis) {
        return redis.opsForZSet().rangeByScore(EXPIRY_SET_KEY, 0, upToEpochMillis);
    }
}
