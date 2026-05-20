package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import java.math.BigDecimal;
import java.util.UUID;

public interface AuctionNotificationPort {
    void publishPriceChange(UUID listingId, BigDecimal newPrice, int bidCount);
    void publishAuctionEnded(UUID listingId, UUID winnerId, BigDecimal finalPrice, String result);
}
