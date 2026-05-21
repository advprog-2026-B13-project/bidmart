package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.AuctionNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuctionEventPublisher implements AuctionNotificationPort {

    private final StringRedisTemplate redis;

    private static final String CHANNEL_PREFIX = "auction:";

    @Async
    public void publishPriceChange(UUID listingId, BigDecimal newPrice, int bidCount) {
        String payload = String.format(
                "{\"type\":\"price-change\",\"listingId\":\"%s\",\"currentPrice\":%s,\"bidCount\":%d}",
                listingId, newPrice.toString(), bidCount);
        redis.convertAndSend(CHANNEL_PREFIX + listingId, payload);
        log.debug("Published price-change for listing {}: price={}, bidCount={}",
                listingId, newPrice, bidCount);
    }

    public void publishAuctionEnded(UUID listingId, UUID winnerId, BigDecimal finalPrice, String result) {
        String payload = String.format(
                "{\"type\":\"auction-ended\",\"listingId\":\"%s\",\"winnerId\":%s,\"finalPrice\":%s,\"result\":\"%s\"}",
                listingId,
                winnerId != null ? "\"" + winnerId + "\"" : "null",
                finalPrice != null ? finalPrice.toString() : "null",
                result);
        redis.convertAndSend(CHANNEL_PREFIX + listingId, payload);
        log.debug("Published auction-ended for listing {}", listingId);
    }
}
