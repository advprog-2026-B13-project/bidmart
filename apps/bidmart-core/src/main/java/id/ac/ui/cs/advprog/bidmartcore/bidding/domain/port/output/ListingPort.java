package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ListingPort {
    ListingInfo getListingInfo(UUID listingId);

    record ListingInfo(
            UUID sellerId,
            ListingStatus status,
            BigDecimal startingPrice,
            BigDecimal currentPrice,
            BigDecimal reservePrice,
            BigDecimal minBidIncrement,
            LocalDateTime startTime,
            LocalDateTime endTime,
            UUID winnerId
    ) {}
}
