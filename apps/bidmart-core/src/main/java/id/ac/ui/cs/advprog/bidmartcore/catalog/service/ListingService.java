package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

public interface ListingService {
    Listing createListing(ListingCreateRequest request, UUID sellerId);
    Listing updateListing(UUID id, AuthContext authContext, ListingUpdateRequest request);
    Listing getListingById(UUID id);

    Listing getListingForOwner(UUID id, UUID ownerId);
    void deleteListing(UUID id, UUID requesterId);

    List<Listing> getListingsBySeller(UUID sellerId);

    Listing activateListing(UUID id, UUID requesterId);

    Listing closeListing(UUID id, UUID requesterId);

    boolean isListingValidForBid(UUID listingId);
    Page<Listing> searchListings(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer categoryId, Pageable pageable);
    Page<Listing> searchListings(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer categoryId, ListingStatus status, Pageable pageable);

    void updateFinalResult(UUID listingId, BigDecimal finalPrice, UUID winnerId);

    void updateCurrentPriceAndWinner(UUID listingId, java.math.BigDecimal newPrice, UUID winnerId);
    void updateStatus(UUID listingId, ListingStatus status);

    void updateEndTime(UUID listingId, LocalDateTime endTime);

    boolean canEditListing(Listing listing, AuthContext authContext);
}