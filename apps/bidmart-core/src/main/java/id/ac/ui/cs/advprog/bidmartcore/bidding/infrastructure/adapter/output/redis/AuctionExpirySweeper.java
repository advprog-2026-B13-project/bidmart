package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionExpirySweeper {

    private final ConcurrencyPort concurrencyPort;
    private final BiddingUseCase biddingService;

    @Scheduled(fixedRate = 1000)
    public void closeExpiredAuctions() {
        long now = System.currentTimeMillis();
        Set<String> expired = concurrencyPort.getExpiredFromExpirySet(now);
        if (expired == null || expired.isEmpty()) return;

        for (String listingIdStr : expired) {
            UUID listingId;
            try {
                listingId = UUID.fromString(listingIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in expiry set: {}", listingIdStr);
                concurrencyPort.removeFromExpirySet(UUID.fromString(listingIdStr));
                continue;
            }

            try {
                biddingService.closeAuction(listingId);
            } catch (Exception e) {
                log.error("Failed to close expired auction {}", listingId, e);
                // Remove anyway — closeAuction has its own guard (checks ACTIVE status)
                // If it fails for a legitimate reason, the auction will be re-added
                // by cacheAuction/refreshAuction when needed
            }
        }
    }
}
