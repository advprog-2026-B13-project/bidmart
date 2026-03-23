package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface ListingPort {
    Listing getListing(UUID listingId);
    void updateCurrentPriceAndWinner(UUID listingId, BigDecimal newPrice, UUID winnerId);
    void updateStatus(UUID listingId, ListingStatus status);
}
