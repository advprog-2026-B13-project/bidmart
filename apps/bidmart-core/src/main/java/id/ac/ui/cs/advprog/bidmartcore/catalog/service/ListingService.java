package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import java.util.UUID;

public interface ListingService {
    Listing createListing(Listing listing);
    Listing updateListing(UUID id, Listing updatedListing);
    Listing getListingById(UUID id);
    void deleteListing(UUID id);
    void updateCurrentPriceAndWinner(UUID listingId, java.math.BigDecimal newPrice, UUID winnerId);
}