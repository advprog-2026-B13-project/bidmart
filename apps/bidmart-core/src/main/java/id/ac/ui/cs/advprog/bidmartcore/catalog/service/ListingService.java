package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {
    Listing createListing(ListingCreateRequest request);
    Listing updateListing(UUID id, UUID requesterId, ListingUpdateRequest request);
    Listing getListingById(UUID id);
    void deleteListing(UUID id, UUID requesterId);

    boolean isListingValidForBid(UUID listingId);
    Page<Listing> searchListings(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer categoryId, Pageable pageable);

    void updateFinalResult(UUID listingId, BigDecimal finalPrice, UUID winnerId);

    void updateCurrentPriceAndWinner(UUID listingId, java.math.BigDecimal newPrice, UUID winnerId);
    void updateStatus(UUID listingId, ListingStatus status);

    void updateEndTime(UUID listingId, LocalDateTime endTime);
}