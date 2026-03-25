package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ListingPort {
    ListingInfo getListingInfo(UUID listingId);
    void updateCurrentPriceAndWinner(UUID listingId, BigDecimal newPrice, UUID winnerId);
    void updateStatus(UUID listingId, ListingStatus status);

    record ListingInfo(
            UUID sellerId,
            ListingStatus status,
            BigDecimal startingPrice,
            BigDecimal currentPrice,
            LocalDateTime endTime
    ) {}
}
