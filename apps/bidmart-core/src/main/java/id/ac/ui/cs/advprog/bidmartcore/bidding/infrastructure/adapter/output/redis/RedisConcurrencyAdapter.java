package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisConcurrencyAdapter implements ConcurrencyPort {

    private final StringRedisTemplate redis;
    private final BidRepositoryPort bidRepository;
    private final RedisScript<List<Object>> placeBidScript;
    private final RedisScript<Long> rollbackScript;

    private static final String KEY_PREFIX = "auction:";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_END_TIME = "endTime";
    private static final String FIELD_WINNER = "winner";
    private static final String FIELD_SELLER_ID = "sellerId";
    private static final String FIELD_STARTING_PRICE = "startingPrice";
    private static final String FIELD_RESERVE_PRICE = "reservePrice";
    private static final String FIELD_MIN_BID_INCREMENT = "minBidIncrement";
    private static final String FIELD_START_TIME = "startTime";
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

        Boolean inserted = redis.opsForHash().putIfAbsent(redisKey, FIELD_STATUS, info.status().name());
        if (Boolean.FALSE.equals(inserted)) {
            return; // already cached, skip
        }

        long price = info.currentPrice() != null
                ? info.currentPrice().longValue()
                : info.startingPrice().longValue();
        long endTime = toEpochMillis(info.endTime());
        String winner = info.winnerId() != null ? info.winnerId().toString() : "";
        long maxAmount = winner.isBlank() ? price : resolveHighestMaxAmount(listingId, price);

        long bidCount = bidRepository.countByListing(listingId);
        redis.opsForHash().putAll(redisKey, Map.of(
                FIELD_PRICE, String.valueOf(price),
                FIELD_END_TIME, String.valueOf(endTime),
                FIELD_STATUS, info.status().name(),
                FIELD_WINNER, winner,
                "maxAmount", String.valueOf(maxAmount),
                FIELD_SELLER_ID, info.sellerId().toString(),
                FIELD_STARTING_PRICE, String.valueOf(info.startingPrice().longValue()),
                FIELD_RESERVE_PRICE, info.reservePrice() != null ? String.valueOf(info.reservePrice().longValue()) : "0",
                FIELD_MIN_BID_INCREMENT, String.valueOf(info.minBidIncrement().longValue()),
                FIELD_START_TIME, String.valueOf(toEpochMillis(info.startTime()))
        ));
        redis.opsForHash().put(redisKey, "bidCount", String.valueOf(bidCount));

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
                FIELD_PRICE, String.valueOf(price),
                FIELD_END_TIME, String.valueOf(endTime),
                FIELD_STATUS, info.status().name(),
                FIELD_WINNER, winner,
                "maxAmount", String.valueOf(maxAmount),
                FIELD_SELLER_ID, info.sellerId().toString(),
                FIELD_STARTING_PRICE, String.valueOf(info.startingPrice().longValue()),
                FIELD_RESERVE_PRICE, info.reservePrice() != null ? String.valueOf(info.reservePrice().longValue()) : "0",
                FIELD_MIN_BID_INCREMENT, String.valueOf(info.minBidIncrement().longValue()),
                FIELD_START_TIME, String.valueOf(toEpochMillis(info.startTime()))
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
    public long incrementAndGetBidCount(UUID listingId) {
        Long count = redis.opsForHash().increment(key(listingId), "bidCount", 1L);
        return count != null ? count : 1L;
    }

    @Override
    public void removeAuction(UUID listingId) {
        redis.delete(key(listingId));
        removeFromExpirySet(listingId);
    }

    @Override
    public long getAuctionEndTime(UUID listingId) {
        Object endTime = redis.opsForHash().get(key(listingId), FIELD_END_TIME);
        return endTime != null ? Long.parseLong(endTime.toString()) : 0L;
    }

    @Override
    public LiveAuctionState getAuctionLiveState(UUID listingId) {
        List<Object> values = redis.opsForHash().multiGet(key(listingId),
                java.util.List.of(FIELD_PRICE, FIELD_WINNER));
        Object price = values.get(0);
        if (price == null) return null;
        String winner = values.get(1) != null ? values.get(1).toString() : "";
        return new LiveAuctionState(Long.parseLong(price.toString()), winner);
    }

    @Override
    public ListingPort.ListingInfo getListingInfoFromCache(UUID listingId) {
        List<Object> values = redis.opsForHash().multiGet(key(listingId), java.util.List.of(
                FIELD_SELLER_ID, FIELD_STATUS, FIELD_STARTING_PRICE, FIELD_PRICE,
                FIELD_RESERVE_PRICE, FIELD_MIN_BID_INCREMENT, FIELD_START_TIME, FIELD_END_TIME, FIELD_WINNER));
        if (values.get(0) == null) return null;
        try {
            UUID sellerId = UUID.fromString(values.get(0).toString());
            ListingStatus status = ListingStatus.valueOf(values.get(1).toString());
            BigDecimal startingPrice = BigDecimal.valueOf(Long.parseLong(values.get(2).toString()));
            BigDecimal currentPrice = BigDecimal.valueOf(Long.parseLong(values.get(3).toString()));
            long reservePriceLong = Long.parseLong(values.get(4).toString());
            BigDecimal reservePrice = reservePriceLong > 0 ? BigDecimal.valueOf(reservePriceLong) : null;
            BigDecimal minBidIncrement = BigDecimal.valueOf(Long.parseLong(values.get(5).toString()));
            LocalDateTime startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(values.get(6).toString())), BUSINESS_ZONE);
            LocalDateTime endTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(values.get(7).toString())), BUSINESS_ZONE);
            String winnerStr = values.get(8) != null ? values.get(8).toString() : "";
            UUID winnerId = winnerStr.isBlank() ? null : UUID.fromString(winnerStr);
            return new ListingPort.ListingInfo(sellerId, status, startingPrice, currentPrice,
                    reservePrice, minBidIncrement, startTime, endTime, winnerId);
        } catch (Exception e) {
            return null;
        }
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
