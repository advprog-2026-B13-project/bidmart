package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BiddingUseCase {
    BidResult placeBid(UUID listingId, BigDecimal amount, UUID bidderId);
    List<BidResult> getBidsForListing(UUID listingId);
    List<BidResult> getMyBids(UUID bidderId);
    AuctionStatusResult getAuctionStatus(UUID listingId);
    void closeAuction(UUID listingId);

    record BidResult(
            UUID bidId,
            UUID listingId,
            UUID bidderId,
            BigDecimal amount,
            String status,
            LocalDateTime createdAt
    ) {}

    record AuctionStatusResult(
            UUID listingId,
            BigDecimal currentPrice,
            UUID currentWinnerId,
            BigDecimal myHighestBid,
            LocalDateTime endTime,
            String status
    ) {}
}
