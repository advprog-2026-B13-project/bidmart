package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import java.time.LocalDateTime;
import java.util.UUID;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

public interface ListingService {
    Listing createListing(Listing listing);
    Listing updateListing(UUID id, Listing updatedListing);
    Listing getListingById(UUID id);
    void deleteListing(UUID id);
    void updateCurrentPriceAndWinner(UUID listingId, java.math.BigDecimal newPrice, UUID winnerId);
    void updateStatus(UUID listingId, ListingStatus status);

    void updateEndTime(UUID listingId, LocalDateTime endTime);
}